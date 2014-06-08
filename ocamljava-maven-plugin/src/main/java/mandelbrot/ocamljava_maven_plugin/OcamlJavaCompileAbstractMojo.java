package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import mandelbrot.ocamljava_maven_plugin.util.ClassPathGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.compilers.ocamljavaMain;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;
import org.ocamljava.dependency.analyzer.Analyzer;
import org.ocamljava.dependency.data.DependencyGraph;
import org.ocamljava.dependency.data.ModuleDescriptor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public abstract class OcamlJavaCompileAbstractMojo extends OcamlJavaAbstractMojo {


	public File chooseDependencyGraphTargetFullPath() {
		return new File(chooseOcamlCompiledSourcesTarget()  + 
				File.separator + dependencyGraphTarget);
	}

	/***
	 * Record debugging information.
	 * 
	 * @parameter default-value="false"
	 * 
	 */
	protected boolean recordDebugInfo = false;
		
	private static final String DOT = ".";
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

			final ImmutableSet<String> intersAndImpls = ImmutableSet.<String>builder()
					.addAll(moduleInterfaces)
					.addAll(implementations).build();
			
			final DependencyGraph dependencyGraph = 
					analyzer.resolveModuleDependenciesByPackageName(intersAndImpls, chooseOcamlSourcesDirectory());

			final File file = chooseDependencyGraphTargetFullPath();
			file.getParentFile().mkdirs();
			
			dependencyGraph.write(file, chooseOcamlSourcesDirectory());
			
			getLog().info("ordered modules: " + dependencyGraph);
			final Set<Entry<String, Collection<ModuleDescriptor>>> entrySet = dependencyGraph.getDependencies().entrySet();
			
			for (final Entry<String, Collection<ModuleDescriptor>> entry : entrySet) {
				compileSources(entry.getValue());
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

	private Collection<String> compileSources(
			final Collection<ModuleDescriptor> moduleDescriptors) throws MojoExecutionException {

		final Collection<String> sourceFiles = Collections2.transform(moduleDescriptors, ModuleDescriptor.toFileTransform());
		final Multimap<String, String> byPathMapping = FileMappings
				.buildPathMap(sourceFiles);

		final Set<String> pathMappings = byPathMapping.keySet();
		final ImmutableSet.Builder<String> builder = ImmutableSet.builder();

		for (final String path : pathMappings) {

			if (!sourceFiles.isEmpty()) {
				final String[] sourceArgs = generateCommandLineArguments(pathMappings,
						toPackage(ocamlSourceDirectory, path), sourceFiles).toArray(new String[] {});
				getLog().info(
						"ocamljava compile args: "
								+ ImmutableList.copyOf(sourceArgs));
				ocamljavaMain.main(sourceArgs);
		
			}
		}
		return builder.build();
	}

	

	private ImmutableSet<String> moveCompiledFiles(final Collection<String> ocamlSourceFiles,
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

						final String compiledSourceName = changeExtension(
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

	private String changeExtension(final File srcFile, final String extension) {
		return srcFile.getName().split("\\" + DOT)[0] + DOT + extension;
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

	public Multimap<String, String> gatherOcamlSourceFiles(final File root) {
		final ImmutableMultimap.Builder<String, String> files = ImmutableMultimap
				.builder();
		if (root.isFile() && isOcamlSourceFile(root)) {
			files.put(org.codehaus.plexus.util.FileUtils.getExtension(root
					.getName()), root.getPath());
			return files.build();
		}

		if (!root.isDirectory() || root.listFiles() == null)
			return files.build();

		for (final File file : root.listFiles()) {
			if (file.isDirectory()) {
				getLog().info("scanning directory: " + file);

				files.putAll(gatherOcamlSourceFiles(file));
			} else {
				if (isOcamlSourceFile(file)) {
					getLog().info("adding ocaml source file: " + file);
					files.put(org.codehaus.plexus.util.FileUtils
							.getExtension(file.getName()), file.getPath());
				}
			}
		}
		return files.build();
	}

	private boolean isOcamlSourceFile(final File file) {
		final String extension = org.codehaus.plexus.util.FileUtils.getExtension(file.getPath());
		if (extension == null)
			return false;
		return OcamlJavaConstants.OCAML_SOURCE_FILE_EXTENSIONS.contains(extension.toLowerCase());
	}

	protected abstract String chooseOcamlCompiledSourcesTarget();
	
	
}
