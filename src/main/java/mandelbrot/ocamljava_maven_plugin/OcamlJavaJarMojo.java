package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import ocaml.compilers.ocamljavaMain;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Goal which compiles ocaml source and test files.
 * 
 * @goal jar
 * 
 * @phase 
 */
public class OcamlJavaJarMojo extends AbstractMojo {

	private static final String IMPL_SOURCE_EXTENSION = "ml";
	private static final String INTERFACE_SOURCE_EXTENSION = "mli";

	private static final String DOT = ".";

	private static final String COMPILED_IMPL_EXTENSION = "cmj";
	private static final String COMPILED_INTERFACE_ENXTENSION = "cmi";
	private static final String OBJECT_BINARY_EXTENSION = "jo";

	private static final Set<String> OCAML_SOURCE_FILE_EXTENSIONS = ImmutableSet
			.of(IMPL_SOURCE_EXTENSION, INTERFACE_SOURCE_EXTENSION);
	
	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;		
	
	/**
	 * Location of the file.
	 * 
	 * @parameter expression="${project.build.directory}"
	 * @required
	 */
	private final File outputDirectory = new File("");

	/**
	 * Project's source directory as specified in the POM.
	 * 
	 * @parameter expression="${project.build.ocamlSourceDirectory}"
	 * @readonly
	 */
	private final File ocamlSourceDirectory = new File("src/main/ocaml");

	/**
	 * Project's source directory as specified in the POM.
	 * 
	 * @parameter expression="${project.build.ocamlTestDirectory}"
	 * @readonly
	 */
	private final File ocamlTestDirectory = new File("src/test/ocaml");
	
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

		project.
		try {
	
			final ImmutableList<String> ocamlSourceFiles = gatherOcamlSourceFiles(ocamlSourceDirectory);
			final String[] sourceArgs = generateCommandLineArguments(ocamlSourceFiles).toArray(new String[]{});

			getLog().info("source args: " + ImmutableList.copyOf(sourceArgs));
			ocamljavaMain.main(sourceArgs);

			moveAllCompiledFiles(ocamlSourceFiles);

			final ImmutableList<String> ocamlTestFiles = gatherOcamlSourceFiles(ocamlTestDirectory);
			final String[] testArgs = generateCommandLineArguments(ocamlTestFiles).toArray(new String[]{});

			getLog().info("test args: " + ImmutableList.copyOf(testArgs));
			ocamljavaMain.main(testArgs);

			moveAllCompiledFiles(ocamlTestFiles);

		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}

	// TODO maintain package path structure when copying to target!
	private void moveAllCompiledFiles(
			final ImmutableList<String> ocamlSourceFiles) {
		moveCompiledSourceFilesToTargetDirectory(ocamlSourceFiles, COMPILED_IMPL_EXTENSION);
		moveCompiledSourceFilesToTargetDirectory(ocamlSourceFiles, COMPILED_INTERFACE_ENXTENSION);
		moveCompiledSourceFilesToTargetDirectory(ocamlSourceFiles, OBJECT_BINARY_EXTENSION);
	}

	private Set<String> moveCompiledSourceFilesToTargetDirectory(final ImmutableList<String> ocamlSourceFiles, final String compiledExtension) {
		final Collection<String> transformed = Collections2.transform(
				ocamlSourceFiles, new Function<String, String>() {

					@Override
					public String apply(final String string) {
						final File srcFile = new File(string);
				
						final String compiledSourceName = changeExtension(srcFile, compiledExtension);
						final File compiledSrcFile = new File(srcFile.getParent() + File.separator + compiledSourceName);
						try {
							if (compiledSrcFile.exists()) {
								getLog().info("moving src " + compiledSrcFile + " to output directory: " + outputDirectory);
								FileUtils.moveFileToDirectory(compiledSrcFile,
										outputDirectory, true);
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
			final ImmutableList<String> ocamlSourceFiles) {
		return ImmutableList.<String>builder().add("-c").addAll(ocamlSourceFiles).build();
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
		return OCAML_SOURCE_FILE_EXTENSIONS.contains(extension);
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
