package mandelbrot.ocamljava_maven_plugin;

import static mandelbrot.ocamljava_maven_plugin.OcamlJavaConstants.DOT;
import static mandelbrot.ocamljava_maven_plugin.OcamlJavaConstants.OCAML_SOURCE_FILE_EXTENSIONS;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import ocaml.compilers.ocamljavaConstants;
import ocaml.compilers.ocamljavaMain;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.sonatype.inject.Description;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Goal which compiles ocaml source and test files.
 * 
 * @goal compile
 * 
 * @phase compile
 * @executionStrategy once-per-session
 * @requiresDependencyResolution
 */
public class OcamlJavaCompileMojo extends OcamlJavaAbstractMojo {

	
	

	@Override
	public void execute() throws MojoExecutionException {

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

		getLog().info("ocaml source directory: " + ocamlSourceDirectory.getAbsolutePath());
	
		try {
	
			final ImmutableList<String> ocamlSourceFiles = gatherOcamlSourceFiles(ocamlSourceDirectory);
			
			if (!ocamlSourceFiles.isEmpty())
			{
				final String[] sourceArgs = generateCommandLineArguments(ocamlSourceFiles, false).toArray(new String[]{});
				getLog().info("source args: " + ImmutableList.copyOf(sourceArgs));
				ocamljavaMain.main(sourceArgs);
				moveAllCompiledFiles(ocamlSourceFiles, ocamlCompiledSourcesTarget, ocamlSourceDirectory.getPath());
			}
			
			final ImmutableList<String> ocamlTestFiles = gatherOcamlSourceFiles(ocamlTestDirectory);
			
			if (!ocamlTestFiles.isEmpty()) {
				final String[] testArgs = generateCommandLineArguments(ocamlTestFiles, true).toArray(new String[]{});
				getLog().info("test args: " + ImmutableList.copyOf(testArgs));
				ocamljavaMain.main(testArgs);
				moveAllCompiledFiles(ocamlTestFiles, ocamlCompiledTestsTarget, ocamlTestDirectory.getPath());
			}
			
		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}

	private void moveAllCompiledFiles(final Collection<String> ocamlSourceFiles, final String outputDirectoryQualifier, final String toFilter) {
		
		for (final String extension: OcamlJavaConstants.OCAML_COMPILED_SOURCE_FILE_EXTENSIONS)
			moveCompiledSourceFilesToTargetDirectory(ocamlSourceFiles, extension, outputDirectoryQualifier, toFilter);
				
	}

	private Set<String> moveCompiledSourceFilesToTargetDirectory(final Collection<String> ocamlSourceFiles, final String compiledExtension,
			final String outputDirectoryQualifier, final String toFilter) {
		final Collection<String> transformed = Collections2.transform(
				ocamlSourceFiles, new Function<String, String>() {

					@Override
					public String apply(final String path) {
						final File srcFile = new File(path);
				
						final String compiledSourceName = changeExtension(srcFile, compiledExtension);
						final File compiledSrcFile = new File(srcFile.getParent() + File.separator + compiledSourceName);
						final File qualifiedOutputDirectory = new File(
								outputDirectory.getPath() + 
								File.separator +
								outputDirectoryQualifier + 
								File.separator + 
								compiledSrcFile.getParent().replace(toFilter, ""));
						
						try {
							if (compiledSrcFile.exists()) {
								getLog().info("moving src " + compiledSrcFile + " to output directory: " + qualifiedOutputDirectory);
								FileUtils.copyFileToDirectory(compiledSrcFile,
										qualifiedOutputDirectory, true);
								FileUtils.deleteQuietly(compiledSrcFile);
							}
							else
								getLog().warn("skipping transfer of file " + compiledSrcFile + " which doesn't exist.");
						} catch (final IOException e) {
							throw new RuntimeException(
									"error moving compiled sources",
									e);
						}
						return outputDirectory.getAbsolutePath()
								+ File.separator + compiledSourceName;
					}
				});
		return ImmutableSet.copyOf(transformed);
	}

	private String changeExtension(final File srcFile, final String extension) {
		return srcFile.getName().split("\\" + DOT)[0] + DOT + extension;
	}

	private ImmutableList<String> generateCommandLineArguments(
			final ImmutableList<String> ocamlSourceFiles, final boolean isTest) throws MojoExecutionException {
		try {
			return ImmutableList.<String>builder()
					.add(OcamlJavaConstants.CLASSPATH_OPTION)
					.add(Joiner.on(";").join(isTest ? project.getTestClasspathElements() : project.getCompileClasspathElements()))
					.add(OcamlJavaConstants.COMPILE_SOURCES_OPTION)
					.addAll(ocamlSourceFiles)
					.build();
		} catch (final DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("dependency resolution required", e);
		}
	}
	
	private boolean ensureTargetDirectoryExists() {
		if (outputDirectory.exists()) {
			return true;
		}
		return outputDirectory.mkdirs();
	}

	public ImmutableList<String> gatherOcamlSourceFiles(final File root) {
		final ImmutableList.Builder<String> files = ImmutableList.builder();
		if (root.isFile() && isOcamlSourceFile(root)) {
			files.add(root.getPath());
			return files.build();
		}
	
		if (!root.isDirectory() || root.listFiles() == null)
			return files.build();
		
		for (final File file : root.listFiles()) {
			if (file.isDirectory()) {
				getLog().info("scanning directory: " + file);
				
				files.addAll(gatherOcamlSourceFiles(file));
			} else {
				if (isOcamlSourceFile(file)) {
					getLog().info("adding ocaml source file: " + file);
					files.add(file.getPath());
				}
			}
		}
		return files.build();
	}

	private boolean isOcamlSourceFile(final File file) {
		final String extension = getExtension(file.getAbsolutePath());
		if (extension == null)
			return false;
		return OCAML_SOURCE_FILE_EXTENSIONS.contains(extension.toLowerCase());
	}

	public static String getExtension(final String filePath) {
		final int dotPos = filePath.lastIndexOf(DOT);
		if (-1 == dotPos) {
			return null;
		} else {
			return filePath.substring(dotPos+1);
		}
	}

}
