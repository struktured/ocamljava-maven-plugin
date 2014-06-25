package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import mandelbrot.dependency.analyzer.Analyzer;
import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.dependency.data.ModuleDescriptor;
import mandelbrot.ocamljava_maven_plugin.util.ClassPathGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileExtensions;
import mandelbrot.ocamljava_maven_plugin.util.FileGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.compilers.ocamljavaMain;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.ocamljava.runtime.kernel.AbstractNativeRunner;
import org.ocamljava.runtime.kernel.FalseExit;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public abstract class OcamlJavaCompileAbstractMojo extends OcamlJavaAbstractMojo {


	/***
	 * Record debugging information.
	 * 
	 * @parameter default-value="false"
	 * 
	 */
	protected boolean recordDebugInfo = false;
		
	/***
	 * Optimize code for size rather than speed.
	 * 
	 * @parameter default-value="false"
	 * 
	 */
	protected boolean compact = false;
		
	public File chooseDependencyGraphTargetFullPath() {
		return new File(chooseOcamlCompiledSourcesTarget()  + 
				File.separator + dependencyGraphTarget);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

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

		getLog().info(
				"ocaml source directory: "
						+ ocamlSourceDirectory.getPath());

		try {

			final Multimap<String, String> ocamlSourceFiles = gatherOcamlSourceFiles(chooseOcamlSourcesDirectory());

			final Collection<String> moduleInterfaces = ocamlSourceFiles
					.get(OcamlJavaConstants.INTERFACE_SOURCE_EXTENSION);

			final Analyzer analyzer = new Analyzer(this);

			final Collection<String> implementations = ocamlSourceFiles
					.get(OcamlJavaConstants.IMPL_SOURCE_EXTENSION);

			final Set<String> intersAndImpls = ImmutableSet.<String>builder()
					.addAll(moduleInterfaces)
					.addAll(implementations).build();
			
			final DependencyGraph dependencyGraph = 
					analyzer.resolveModuleDependenciesByPackageName(intersAndImpls, chooseOcamlSourcesDirectory());

			final File file = chooseDependencyGraphTargetFullPath();
			
			final boolean madeDirs = file.getParentFile().mkdirs();
			
			if (getLog().isDebugEnabled()) {
				getLog().debug("made directory \"" + file + "\"? " + madeDirs);
			}
			
			dependencyGraph.write(file, chooseOcamlSourcesDirectory());
			
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
		
			moveCompiledFiles(implementations, chooseOcamlCompiledSourcesTarget(),
					chooseOcamlSourcesDirectory().getPath(), 
					ImmutableSet.of(OcamlJavaConstants.COMPILED_IMPL_EXTENSION, 
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
						.addAll(includeDirs)
						.addAll(pathMappings)
						.build(),
						toPackage(ocamlSourceDirectory, path), sourceFiles).toArray(new String[] {});
				getLog().info("ocamljava compile args: " + ImmutableList.copyOf(sourceArgs));
				final ocamljavaMain main = ocamljavaMain.mainWithReturn(sourceArgs);
				final Field declaredField = getExceptionField();
				final boolean accessible = declaredField.isAccessible();
				try {
					declaredField.setAccessible(true);
					final Throwable exception = (Throwable) declaredField.get(main);
							
					if (exception != null) {
						if (exception instanceof FalseExit) {
							final FalseExit f = (FalseExit) exception; 
							switch (f.getExitCode()) {
							case 0:
								break;
							default:
							throw new MojoExecutionException("error compiling sources (exit code = " + 
									f.getExitCode() + ", path = " + path + ")");
						} 
					} else throw new MojoExecutionException("error compiling sources (path = " + path + ")", exception);
						 	
				}

				} finally {
					declaredField.setAccessible(accessible);
					main.clearException();
				}
			}
		}
		 
		return builder.build();
	}

	// This seems to be only the way to access the exception protected field
	// from the ocaml main object at this time.
	private static Field getExceptionField() throws NoSuchFieldException {
		return AbstractNativeRunner.class.getDeclaredField("exception");
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

						try {
							if (compiledSrcFile.exists()) {
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

		for (final String includePath : includePaths) {
			if (!StringUtils.isBlank(includePath)) {
				builder.add(OcamlJavaConstants.INCLUDE_DIR_OPTION).add(
						includePath);
			}

		}

		builder.add(OcamlJavaConstants.CLASSPATH_OPTION)
				.add(Joiner.on(";").join(
						ImmutableSet
								.builder()
								.addAll(new ClassPathGatherer(this)
										.getClassPath(project, false)).build()))
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

	protected Multimap<String, String> gatherOcamlSourceFiles(final File root) {
		return new FileGatherer(this).gatherFiles(root, OcamlJavaConstants.OCAML_SOURCE_FILE_EXTENSIONS); 
	}

	protected abstract String chooseOcamlCompiledSourcesTarget();
	
}
