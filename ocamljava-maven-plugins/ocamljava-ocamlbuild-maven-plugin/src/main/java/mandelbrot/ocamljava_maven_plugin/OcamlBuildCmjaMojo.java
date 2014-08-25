package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import mandelbrot.dependency.analyzer.Analyzer;
import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.dependency.data.ModuleDescriptor;
import mandelbrot.ocamljava_maven_plugin.util.ArtifactDescriptor;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import mandelbrot.ocamljava_maven_plugin.util.JarExtractor;
import mandelbrot.ocamljava_maven_plugin.util.StringTransforms;
import ocaml.compilers.ocamljavaMain;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * <p>
 * This is a goal which executes ocamlbuild jobs. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamlbuild foobar.cmja</code>
 * </p>
 * from the command line but instead uses maven parameters to infer the compiled
 * module interface locations. All parameters can be overridden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @since 1.0
 */
@Mojo(name = "cmja", defaultPhase = LifecyclePhase.PACKAGE, 
	threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME, requiresProject = true)
public class OcamlBuildCmjaMojo extends OcamlBuildAbstractMojo {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final String OCAMLJAVA_MAVEN_PLUGIN_COMMAND_LINE_ARGS = "ocamljava.maven.plugin.commandLineArgs";

	/***
	 * <p>
	 * generation on. Expressed with
	 * groupId:artifactId:version[:type[:classifier]] format.
	 * </p>
	 * 
	 * <p>
	 * NOTE: The artifacts must have been packaged as either a jar or zip file.
	 * </p>
	 */
	@Parameter(required = true)
	protected Set<String> targetArtifacts = ImmutableSet.of();

	/***
	 * Prefix for names of generated classes. Default value is blank.
	 * 
	 */
	@Parameter(defaultValue = "")
	protected String classNamePrefix = "";

	/***
	 * Suffix for names of generated classes.
	 * 
	 */
	@Parameter(defaultValue = "Wrapper")
	protected String classNameSuffix = "Wrapper";

	/***
	 * Arguments passed for library initialization. Defaults to empty.
	 * 
	 */
	@Parameter(defaultValue = "")
	protected String libraryArgs = "";

	public static enum LibraryInitMode {
		STATIC, EXPLICIT;

		public String toCommandLineValue() {
			return name().toLowerCase();
		}
	}

	/**
	 * Library initialization mode. One of <code>EXPLICIT</code> or
	 * <code>STATIC</code>.
	 * 
	 */
	@Parameter(defaultValue = "EXPLICIT")
	protected LibraryInitMode libraryInitMode = LibraryInitMode.EXPLICIT;

	/***
	 * Library package.
	 * 
	 */
	@Parameter(defaultValue = "")
	protected String libaryPackage = "";

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {


		if (targetArtifacts == null || targetArtifacts.isEmpty()) {
			getLog().info("no artifacts to wrap");
			return;
		}

		final Properties properties = System.getProperties();
		final Object object = properties.get(FORK_PROPERTY_NAME);

		if (Boolean.parseBoolean(Optional.fromNullable(object).or(Boolean.TRUE)
				.toString())) {
			getLog().info("forking process");

			final boolean forkAgain = false;
			invokePlugin(fullyQualifiedGoal(), forkAgain);

		} else {
			final Collection<String> artifactFiles = getArtifactFiles();

			final Multimap<String, String> filesByExtension = extractFilesFromArtifacts(artifactFiles);

			if (filesByExtension.isEmpty()) {
				getLog().info(
						"no relevant files found in " + getTargetJarFullPath());
				return;
			}

			final DependencyGraph dependencyGraph = getDependendyGraph(filesByExtension);

			getLog().info("dependencyGraph: " + dependencyGraph);

			final ImmutableSet<String> compiledFiles = ImmutableSet
					.<String> builder()
					.addAll(filesByExtension
							.get(OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION))
					.addAll(filesByExtension
							.get(OcamlJavaConstants.COMPILED_IMPL_JAVA_EXTENSION))
					.build();

			if (compiledFiles.isEmpty()) {
				getLog().info(
						"no wrappable files found in " + getTargetJarFullPath());
				return;
			}

			runOcamlBuild(FileMappings.buildPathMap(artifactFiles).values(), dependencyGraph.getModuleDescriptors());

		}
	}

	private String[] getCommandLineArgs() throws MojoExecutionException {

		try {
			final List<String> arguments = OBJECT_MAPPER.readValue(
					getCommandLineArgumentsFile(),
					new TypeReference<List<String>>() {
					});
			return arguments.toArray(new String[] {});
		} catch (final IOException e) {
			throw new MojoExecutionException(
					"failed to get command line arguments for ocamlwrap invocation",
					e);
		}
	}

	private File getCommandLineArgumentsFile() {
		final File file = new File(getOcamlCompiledSourcesTargetFullPath()
				+ File.separator + "commandLineArgs."
				+ OcamlJavaConstants.JSON_EXTENSION);
		file.getParentFile().mkdirs();
		return file;
	}

	private boolean hasCommandLineArgs() {
		final String property = System
				.getProperty(OCAMLJAVA_MAVEN_PLUGIN_COMMAND_LINE_ARGS);
		if (StringUtils.isBlank(property))
			return false;

		return Boolean.parseBoolean(property);
	}





	private static Collection<String> filterFiles(
			final Collection<String> filesInPackage) {
		final Collection<String> cmiFiles = Collections2.filter(filesInPackage,
				new Predicate<String>() {
					@Override
					public boolean apply(final String value) {
						return OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION
								.equalsIgnoreCase(FileUtils.extension(value));
					}
				});
		return cmiFiles;
	}

	public String fullyQualifiedGoal() {
		return OcamlJavaConstants.wrapGoal();
	}

	private Comparator<? super String> createComparator(
			final Collection<ModuleDescriptor> moduleDescriptors) {
		final Comparator<? super String> comparator = new Comparator<String>() {
			@Override
			public int compare(final String paramT1, final String paramT2) {
				if (Objects.equal(paramT1, paramT2)) {
					return 0;
				}

				for (final ModuleDescriptor moduleDescriptor : moduleDescriptors) {

					final Optional<String> moduleNameOfSource = Analyzer
							.moduleNameOfSource(paramT1);
					if (Objects.equal(moduleDescriptor.getModuleName(),
							moduleNameOfSource.orNull())) {
						return -1;
					}

					final Optional<String> moduleNameOfSource2 = Analyzer
							.moduleNameOfSource(paramT2);
					if (Objects.equal(moduleDescriptor.getModuleName(),
							moduleNameOfSource2.orNull())) {
						return 1;
					}
				}

				return Optional.fromNullable(paramT1).or("").compareTo(paramT2);
			}
		};
		return comparator;
	}

	

	private String inferGeneratedSourceName(final String cmiFile) {
		final String generatedSource = StringTransforms.trim(
				FileUtils.basename(new File(cmiFile).getName()), ".");

		return Optional.fromNullable(this.classNamePrefix).or("")
				+ StringUtils.capitalizeFirstLetter(generatedSource)
				+ Optional.fromNullable(this.classNameSuffix).or("")
				+ OcamlJavaConstants.JAVA_EXTENSION;
	}

	private Multimap<String, String> extractFilesFromArtifacts(
			final Collection<String> artifactFiles) {

		final ImmutableMultimap.Builder<String, String> moduleFilesBuilder = ImmutableMultimap
				.builder();

		for (final String artifactFile : artifactFiles) {
			final Collection<String> compiledModules = new JarExtractor(this)
					.extractFiles(
							artifactFile,
							getOcamlCompiledSourcesTargetFullPath(),
							ImmutableSet
									.of(OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION,
											OcamlJavaConstants.COMPILED_IMPL_OCAML_EXTENSION,
											OcamlJavaConstants.COMPILED_IMPL_JAVA_EXTENSION,
											OcamlJavaConstants.JSON_EXTENSION));

			for (final String compiledModule : compiledModules) {
				moduleFilesBuilder.put(FileUtils.getExtension(compiledModule),
						compiledModule);
			}
		}

		return moduleFilesBuilder.build();
	}

	private Collection<String> getArtifactFiles() {
		final ImmutableList.Builder<String> builder = ImmutableList.builder();

		final Collection<Artifact> filter = Collections2.filter(
				project.getArtifacts(), new Predicate<Artifact>() {
					@Override
					public boolean apply(final Artifact artifact) {

						final String artifactDescription = ArtifactDescriptor
								.toDescriptor(artifact);
						getLog().info("artifact key: " + artifactDescription);
						for (final String targetArtifact : targetArtifacts)
							if (artifactDescription.startsWith(targetArtifact))
								return true;

						return false;
					}

				});

		for (final Artifact artifact : filter) {
			getLog().info("adding artifact: " + artifact);
			builder.add(artifact.getFile().getPath());
		}

		final ImmutableList<String> artifactFiles = builder.build();
		return artifactFiles;
	}

	private Optional<String[]> generateCommandLineArguments(
			final Collection<String> includePaths,
			final Collection<String> files) {
		if (files == null || files.isEmpty())
			return Optional.absent();

		final ImmutableList.Builder<String> builder = ImmutableList.builder();

		if (!includePaths.isEmpty()) {
			builder.add(OcamlJavaConstants.INCLUDE_DIRS_OPTION);
			builder.add(Joiner.on(OcamlJavaConstants.INCLUDE_DIR_SEPARATOR).join(includePaths));
		}
		
		if (!StringUtils.isBlank(classNamePrefix)) {
			builder.add(OcamlJavaConstants.CLASS_NAME_PREFIX_OPTION);
			builder.add(classNamePrefix);
		}

		if (!StringUtils.isBlank(classNameSuffix)) {
			builder.add(OcamlJavaConstants.CLASS_NAME_SUFFIX_OPTION);
			builder.add(classNameSuffix);
		}

		if (libraryInitMode != null) {
			builder.add(OcamlJavaConstants.LIBRARY_INIT_OPTION);
			builder.add(libraryInitMode.toCommandLineValue());
		}

		if (!StringUtils.isBlank(libraryArgs)) {
			builder.add(OcamlJavaConstants.LIBRARY_ARGS_OPTION);
			builder.add(String.format("\"%s\"", libraryArgs));
		}

		if (!StringUtils.isBlank(libaryPackage)) {
			builder.add(OcamlJavaConstants.LIBRARY_PACKAGE_OPTION);
			builder.add(libaryPackage);
		}


		builder.addAll(files);
		return Optional.of(builder.build().toArray(new String[] {}));
	}

	@Override
	protected String chooseTargetJar() {
		return targetJar;
	}

	@Override
	protected String chooseTargetOcamlJar() {
		return targetOcamlJar;
	}

	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

	private Collection<String> runOcamlBuild(final Collection<String> includeDirs,
			final Collection<ModuleDescriptor> moduleDescriptors) throws MojoExecutionException {

		final Collection<String> sourceFiles = Collections2.transform(moduleDescriptors, ModuleDescriptor.toFileTransform());
		final Multimap<String, String> byPathMapping = FileMappings
				.buildPathMap(sourceFiles);

		final Set<String> pathMappings = byPathMapping.keySet();
		final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
		final Predicate<String> predicate = new Predicate<String>() { 
							@Override public boolean apply(final String input) { return new File(input).exists(); }};
	    final Collection<String> filteredIncludeDirs = Collections2.filter(includeDirs, predicate);
	    final Collection<String> filteredPathMappings = Collections2.filter(pathMappings, predicate);

		for (final String path : filteredPathMappings) {
		   			if (!sourceFiles.isEmpty()) {
				final ImmutableSet<String> allIncludes = ImmutableSet.<String>builder()
								.addAll(filteredIncludeDirs)
								.addAll(filteredPathMappings)
								.build();
				final String[] sourceArgs = generateCommandLineArguments(allIncludes,
						 sourceFiles).or(new String[] {});
				if (sourceArgs.length == 0)
					continue;
				
				if (getLog().isInfoEnabled())
					getLog().info("ocamljava compile args: " + Joiner.on(" ").join(ImmutableList.copyOf(sourceArgs)));
				
				final ocamljavaMain main = ocamljavaMain.mainWithReturn(sourceArgs);
				checkForErrors("ocamljava compiler error while processing path: " + path, main);
			}
		}
		 
		return builder.build();
	}

}
