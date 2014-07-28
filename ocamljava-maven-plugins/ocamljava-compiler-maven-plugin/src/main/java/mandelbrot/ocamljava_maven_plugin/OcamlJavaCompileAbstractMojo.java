package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.dependency.data.ModuleDescriptor;
import mandelbrot.ocamljava_maven_plugin.util.ArtifactDescriptor;
import mandelbrot.ocamljava_maven_plugin.util.ClassPathGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileExtensions;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.compilers.ocamljavaMain;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public abstract class OcamlJavaCompileAbstractMojo extends OcamlJavaAbstractMojo {


	private static final Boolean FORK_BY_DEFAULT = Boolean.FALSE;

	/**
	 * Record debugging information.
	 * 
	 */
    @Parameter(defaultValue="false")
	protected boolean recordDebugInfo = false;
		
	/**
	 * Optimize code for size rather than speed.
	 *
	 */
    @Parameter(defaultValue="false")
	protected boolean compact = false;

	@Override
	public final void execute() throws MojoExecutionException, MojoFailureException {

		if (!ensureTargetDirectoryExists()) {
			getLog().error("Could not create target directory");
			return;
		}

		if (!ocamlSourceDirectory.exists()) {
			getLog().error(
					"Source directory \"" + ocamlSourceDirectory
							+ "\" is not valid.");
			return;
		}

		
		final Object object = System.getProperty(FORK_PROPERTY_NAME);
		
		if (Boolean.parseBoolean(Optional.fromNullable(object).or(FORK_BY_DEFAULT)
				.toString())) {
			getLog().info("forking process");

			final boolean forkAgain = false;
			invokePlugin(fullyQualifiedGoal(), forkAgain);
			return;
		} 

		getLog().info(
				"ocaml source directory: "
						+ ocamlSourceDirectory.getPath());

		try {

			final Multimap<String, String> ocamlSourceFiles = gatherOcamlSourceFiles(chooseOcamlSourcesDirectory());

			final File dependencyGraphTarget = chooseDependencyGraphTargetFullPath();
		
			if (getLog().isInfoEnabled())
				getLog().info("full path for dependency target: " + dependencyGraphTarget.getPath());
			
			final boolean madeDirs = dependencyGraphTarget.getParentFile().mkdirs();
			
			if (getLog().isDebugEnabled()) {
				getLog().debug("made directory \"" + dependencyGraphTarget + "\"? " + madeDirs);
			}
			
			final DependencyGraph dependencyGraph = DependencyGraph.read(dependencyGraphTarget);
			
			if (getLog().isInfoEnabled())
				getLog().info("ordered modules: " + dependencyGraph);
			
			final Set<Entry<String, Collection<ModuleDescriptor>>> entrySet = dependencyGraph.getDependencies().entrySet();
			
			final ImmutableSet.Builder<String> includeDirectoryBuilder = ImmutableSet.builder();
		
			for (final Entry<String, Collection<ModuleDescriptor>> entry : entrySet) {
				compileSources(includeDirectoryBuilder.build(), entry.getValue());
				includeDirectoryBuilder.addAll(Collections2.transform(entry.getValue(), new Function<ModuleDescriptor, String>() {
					@Override public String apply(final ModuleDescriptor moduleDescriptor) {
						return moduleDescriptor.getModuleFile().get().getParent();
					}
				}));
			}
		
			moveCompiledFiles(ocamlSourceFiles.get(OcamlJavaConstants.IMPL_SOURCE_EXTENSION), chooseOcamlCompiledSourcesTarget(),
					chooseOcamlSourcesDirectory().getPath(), 
					ImmutableSet.of(OcamlJavaConstants.COMPILED_IMPL_JAVA_EXTENSION, 
							OcamlJavaConstants.OBJECT_BINARY_EXTENSION, OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION));

		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}

	protected abstract File chooseOcamlSourcesDirectory();

	private Collection<String> compileSources(final Collection<String> includeDirs,
			final Collection<ModuleDescriptor> moduleDescriptors) throws MojoExecutionException, IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {

		final Collection<String> sourceFiles = Collections2.transform(moduleDescriptors, ModuleDescriptor.toFileTransform());
		final Multimap<String, String> byPathMapping = FileMappings
				.buildPathMap(sourceFiles);

		final Set<String> pathMappings = byPathMapping.keySet();
		final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

		for (final String path : pathMappings) {

			if (!sourceFiles.isEmpty()) {
				final String[] sourceArgs = generateCommandLineArguments(ImmutableSet.<String>builder()
						.addAll(Collections2.filter(includeDirs, 
								new Predicate<String>() { 
							@Override public boolean apply(final String input) { return new File(input).exists(); }}))
						.addAll(pathMappings)
						.build(),
						toPackage(ocamlSourceDirectory, path), sourceFiles).toArray(new String[] {});
				
				if (getLog().isInfoEnabled())
					getLog().info("ocamljava compile args: " + Joiner.on(" ").join(ImmutableList.copyOf(sourceArgs)));
				
				final ocamljavaMain main = ocamljavaMain.mainWithReturn(sourceArgs);
				checkForErrors("ocamljava compiler error while processing path: " + path, main);
			}
		}
		 
		return builder.build();
	}

	
	private Set<String> moveCompiledFiles(final Collection<String> ocamlSourceFiles,
			final String outputDirectoryQualifier, final String toFilter, final Set<String> extensions) {

		final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
	
		for (final String extension : extensions)
			builder.addAll(moveCompiledSourceFilesToTargetDirectory(ocamlSourceFiles,
					extension, outputDirectoryQualifier, toFilter));
		return builder.build();

	}

	private Set<String> moveCompiledSourceFilesToTargetDirectory(
			final Collection<String> ocamlSourceFiles,
			final String compiledExtension,
			final String outputDirectoryQualifier, final String toFilter) {
		final Collection<String> transformed = Collections2.transform(
				ocamlSourceFiles, new Function<String, String>() {

					@Override
					public String apply(final String path) {
						final File srcFile = new File(path);

						final String compiledSourceName = FileExtensions.changeExtension(
								srcFile, compiledExtension);
						final File compiledSrcFile = new File(srcFile
								.getParent()
								+ File.separator
								+ compiledSourceName);
						final File qualifiedOutputDirectory = new File(
								outputDirectory.getPath()
										+ File.separator
										+ outputDirectoryQualifier
										+ File.separator
										+ compiledSrcFile.getParent().replace(
												toFilter, ""));

						qualifiedOutputDirectory.mkdirs();
						
						try {
							if (compiledSrcFile.exists()) {
								if (getLog().isInfoEnabled())
									getLog().info(
										"moving src " + compiledSrcFile
												+ " to output directory: "
												+ qualifiedOutputDirectory);
								
								
								FileUtils.copyFileToDirectory(compiledSrcFile,
										qualifiedOutputDirectory, true);
								FileUtils.deleteQuietly(compiledSrcFile);
							} else
								getLog().warn(
										"skipping transfer of file "
												+ compiledSrcFile
												+ " which doesn't exist.");
						} catch (final IOException e) {
							throw new RuntimeException(
									"error moving compiled sources", e);
						}
						return outputDirectory.getPath()
								+ File.separator + compiledSourceName;
					}
				});
		return ImmutableSet.copyOf(transformed);
	}

	private List<String> generateCommandLineArguments(
			final Collection<String> includePaths, final String packageName,
			final Collection<String> ocamlSourceFiles)
			throws MojoExecutionException {

		final ImmutableList.Builder<String> builder = ImmutableList
				.<String> builder();

		if (javaExtensions)
			builder.add("javalib.cmja");
		
		if (recordDebugInfo) {
			builder.add(OcamlJavaConstants.RECORD_DEBUGGING_INFO_OPTION);
		}
		
		if (javaExtensions) {
			builder.add(OcamlJavaConstants.JAVA_EXTENSIONS_OPTION);
		}

		if (compact) {
			builder.add(OcamlJavaConstants.COMPACT_OPTION);
		}
		
		if (!StringUtils.isBlank(packageName)) {
			builder.add(OcamlJavaConstants.JAVA_PACKAGE_OPTION)
					.add(packageName);
		}

		addIncludePaths(includePaths, builder);

	    final ImmutableSet<String> classPathElements = ImmutableSet.<String>builder()
	    		 .addAll(new ClassPathGatherer(this).getClassPath(project, false)).build();;
		
	    for (final String classPath : classPathElements) {
			builder
				.add(OcamlJavaConstants.CLASSPATH_OPTION)
				.add(classPath);
		}
	    
		builder
				.add(OcamlJavaConstants.COMPILE_SOURCES_OPTION)
				.addAll(ocamlSourceFiles);
		return builder.build();
	}


	private boolean ensureTargetDirectoryExists() {
		if (outputDirectory.exists()) {
			return true;
		}
		return outputDirectory.mkdirs();
	}

	protected abstract String chooseOcamlCompiledSourcesTarget();

	public abstract String fullyQualifiedGoal();
}
