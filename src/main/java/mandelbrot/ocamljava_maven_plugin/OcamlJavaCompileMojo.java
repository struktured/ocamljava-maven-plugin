package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.util.Set;

import ocaml.compilers.ocamljavaMain;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Goal which counts the total lines of code
 * 
 * @goal compile
 * 
 * @phase compile
 */
public class OcamlJavaCompileMojo extends AbstractMojo {

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
	
		try {
	
			final ImmutableList<String> ocamlSourceFiles = gatherOcamlSourceFiles(ocamlSourceDirectory);
			final String[] sourceArgs = generateCommandLineArguments(ocamlSourceFiles).toArray(new String[]{});

			getLog().info("source args: " + ImmutableList.copyOf(sourceArgs));
			ocamljavaMain.main(sourceArgs);
		
			final ImmutableList<String> ocamlTestFiles = gatherOcamlSourceFiles(ocamlTestDirectory);
			final String[] testArgs = generateCommandLineArguments(ocamlTestFiles).toArray(new String[]{});

			ocamljavaMain.main(testArgs);

		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
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

	private static final Set<String> OCAML_SOURCE_FILE_EXTENSIONS = ImmutableSet
			.of("ml", "mli");

	private boolean isOcamlSourceFile(final File file) {
		final String extension = getExtension(file.getAbsolutePath());
		if (extension == null)
			return false;
		return OCAML_SOURCE_FILE_EXTENSIONS.contains(extension);
	}

	public static String getExtension(final String filePath) {
		final int dotPos = filePath.lastIndexOf(".");
		if (-1 == dotPos) {
			return null;
		} else {
			return filePath.substring(dotPos+1);
		}
	}

}
