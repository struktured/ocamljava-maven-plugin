package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import mandelbrot.dependency.analyzer.Analyzer;
import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.dependency.data.ModuleDescriptor;
import mandelbrot.ocamljava_maven_plugin.util.ArtifactDescriptor;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import mandelbrot.ocamljava_maven_plugin.util.JarExtractor;
import mandelbrot.ocamljava_maven_plugin.util.MapTransforms;
import mandelbrot.ocamljava_maven_plugin.util.StringTransforms;

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
import org.ocamljava.wrapper.ocamljavaMain;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * <p>
 * This is a goal which wraps OCaml compiled source interfaces with code
 * generated java equivalents. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamlwrap -package com.mycomp lib.cmi</code>
 * </p>
 * from the command line but instead uses maven parameters to infer the compiled
 * module interface locations. All parameters can be overridden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @since 1.0
 */
@Mojo(name = "wrap", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME, requiresProject = true)
public class OcamlWrapMojo extends OcamlJavaWrapAbstractMojo {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private static final String OCAMLJAVA_MAVEN_PLUGIN_COMMAND_LINE_ARGS = "ocamljava.maven.plugin.commandLineArgs";

	/***
	 * The working directory where the generated Java source files are created.
	 * 
	 */
	@Parameter(required = true, defaultValue = "${project.build.directory}/generated-sources/ocaml")
	protected File generatedSourcesOutputDirectory;

	/***
	 * <p>
	 * The artifacts to scan for ocaml compiled interfaces and then perform code
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

	/***
	 * Whether to disable warnings during the code generation process.
	 * 
	 */
	@Parameter(defaultValue = "false")
	protected boolean noWarnings;

	public static enum StringMapping {
		JAVA_STRING, OCAMLSTRING, BYTE_ARRAY;
		public String toCommandLineValue() {
			return name().toLowerCase().replace("_", "-");
		}
	}

	/***
	 * Determines the string mapping for the OCaml string type. One of
	 * <code>JAVA_STRING</code>, <code>OCAMLSTRING</code>, or
	 * <code>BYTE-ARRAY</code>.
	 * 
	 */
	@Parameter(defaultValue = "JAVA_STRING")
	protected StringMapping stringMapping = StringMapping.JAVA_STRING;

	/***
	 * Whether to enable verbose mode.
	 * 
	 **/
	@Parameter(defaultValue = "false")
	protected boolean verbose;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (hasCommandLineArgs()) {
			final ocamljavaMain result = org.ocamljava.wrapper.ocamljavaMain
					.mainWithReturn(getCommandLineArgs());
			// Will never execute, because the above forks.
			getLog().warn("process should have exited: " + result);
			return;
		}

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

			wrapInternal(dependencyGraph, compiledFiles);

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

	private void wrapInternal(final DependencyGraph dependencyGraph,
			final Set<String> compiledFiles) throws MojoExecutionException {
		getLog().info("infer package names based on directory structure");

		final Multimap<String, String> filesByPackageName = FileMappings
				.buildPackageMap(new File(
						getOcamlCompiledSourcesTargetFullPath()), compiledFiles);

		final Map<String, String> packageNamesByFile = MapTransforms.reverseIndex(filesByPackageName);

		getLog().info("filesByPackageName: " + filesByPackageName);

		final ImmutableSet.Builder<String> includeDirsBuilder = ImmutableSet
				.builder();

		// TODO inefficient - also should be factored into a util library module
		for (final String file : packageNamesByFile.keySet()) {
			try {
				includeDirsBuilder.add(new File(file).getParent());

			} catch (final Exception e) {
				getLog().error("wrapping threw an exception", e);
				throw new MojoExecutionException("wrapping threw an exception",
						e);
			}
		}
		final Set<ModuleDescriptor> concat = ImmutableSet.copyOf(Iterables
				.concat(dependencyGraph.getDependencies().values()));

		final Comparator<? super String> comparator = createComparator(concat);

		final ImmutableSortedSet<String> sorted = ImmutableSortedSet.copyOf(
				comparator, filterFiles(filesByPackageName.values()));

		wrapFiles(includeDirsBuilder.build(), sorted, packageNamesByFile);
		moveFiles(sorted);

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

	private void wrapFiles(final Collection<String> includeDirs,
			final Collection<String> cmiFiles, final Map<String, String> packageNamesByFile) throws MojoExecutionException {

		final Optional<String[]> commandLineArguments = generateCommandLineArguments(
				includeDirs, cmiFiles, packageNamesByFile);

		if (commandLineArguments.isPresent()) {
			final ImmutableList<String> argsAsList = ImmutableList
					.copyOf(commandLineArguments.get());

			if (getLog().isInfoEnabled())
				getLog().info(
						"command line arguments: "
								+ Joiner.on(" ").join(argsAsList));

			final File commandLineArgumentsFile = getCommandLineArgumentsFile();

			try {
				OBJECT_MAPPER.writeValue(commandLineArgumentsFile, argsAsList);
			} catch (final IOException e) {
				throw new MojoExecutionException(
						"exception seriliazing command line arguments: "
								+ argsAsList, e);
			}

			final String goal = fullyQualifiedGoal();
			final boolean forkAgain = false;
			final Properties properties = new Properties();
			properties.put(OCAMLJAVA_MAVEN_PLUGIN_COMMAND_LINE_ARGS,
					Boolean.TRUE.toString());

			invokePlugin(goal, forkAgain, properties);

		} else if (getLog().isInfoEnabled()) {
			getLog().info(
					"no compiled module interfaces to wrap for package \""
							+ packageName + "\" in "
							+ getOcamlCompiledSourcesTargetFullPath());
		}
	}

	private void moveFiles(final Collection<String> cmiFiles) {

		for (final String cmiFile : cmiFiles) {

			try {

				final String generatedSourceName = inferGeneratedSourceName(cmiFile);

				final String packagePath = isDynamicPackageMode() ? FileMappings
						.toPackagePath(getOcamlCompiledSourcesTargetFullPath(),
								cmiFile) : packageName;
				final String target =
				// eg.
				// target/generate-sources/ocaml/com/mycomp/FooWrapper.java
				this.generatedSourcesOutputDirectory
						+ (StringUtils.isNotBlank(packagePath) ? (File.separator + packagePath)
								: "");

				// TODO ..should I just scan for *.java instead, or some
				// regex pattern specified in Maven project?
				final File file = new File(project.getBasedir().getPath()
						+ File.separator + generatedSourceName);

				if (!file.exists()) {
					getLog().warn(
							"expected file " + file
									+ " but it does not exist! skipping...");
					continue;
				}

				final File targetDir = new File(target);
				getLog().info("copying " + file + " to " + targetDir);

				FileUtils.copyFileToDirectory(file, targetDir);
			} catch (final IOException e) {
				getLog().error("io exception", e);
			}
		}
		return;
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
			final Collection<String> files,
			final Map<String, String> packageNames) {
		if (files == null || files.isEmpty())
			return Optional.absent();

		final ImmutableList.Builder<String> builder = ImmutableList.builder();

		if (noWarnings)
			builder.add(OcamlJavaConstants.NO_WARNINGS_OPTION);

		if (verbose)
			builder.add(OcamlJavaConstants.VERBOSE_OPTION);

		for (final String includePath : includePaths) {
			if (!StringUtils.isBlank(includePath)) {
				builder.add(OcamlJavaConstants.INCLUDE_DIR_OPTION).add(
						includePath);
			}
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

		if (!StringUtils.isBlank(packageName) && !isDynamicPackageMode()) {
			builder.add(OcamlJavaConstants.PACKAGE_OPTION);
			builder.add(this.packageName);
		}

		if (stringMapping != null) {
			builder.add(OcamlJavaConstants.STRING_MAPPING_OPTION);
			builder.add(stringMapping.toCommandLineValue());
		}

		if (isDynamicPackageMode()) {
			builder.addAll(Collections2.transform(files,
					new Function<String, String>() {
						@Override
						public String apply(final String file) {
							return fileAtPackage(file, packageNames.get(file));
						}
					}));
		} else {
			builder.addAll(files);
		}
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

}
