package mandelbrot.ocamljava_maven_plugin;

import static mandelbrot.ocamljava_maven_plugin.OcamlConstants.*;

import java.io.File;

import ocaml.compilers.ocamljavaMain;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.google.common.collect.ImmutableList;

/**
 * Goal which compiles ocaml source and test files.
 * 
 * @goal jar
 * 
 * @phase package
 */
public class OcamlJavaJarMojo extends AbstractMojo {


	
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

	
	@Override
	public void execute() throws MojoExecutionException {

		if (!ensureTargetDirectoryExists()) {
			getLog().error("Could not create target directory");
			return;
		}
		
		try {
	
			final ImmutableList<String> ocamlCompiledSourceFiles = gatherOcamlCompiledSources(outputDirectory);
			final String[] args = generateCommandLineArguments(ocamlCompiledSourceFiles).toArray(new String[]{});

			getLog().info("args: " + ImmutableList.copyOf(args));
			ocamljavaMain.main(args);
//
//			final ImmutableList<String> ocamlTestFiles = gatherOcamlCompiledSources(outputDirectory);
//			final String[] testArgs = generateCommandLineArguments(ocamlTestFiles).toArray(new String[]{});
//
//			getLog().info("test args: " + ImmutableList.copyOf(testArgs));
//			ocamljavaMain.main(testArgs);

		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}

	
	private ImmutableList<String> generateCommandLineArguments(
			final ImmutableList<String> ocamlSourceFiles) {
		return ImmutableList.<String>builder().add("-o").addAll(ocamlSourceFiles).build();
	}
	
	private boolean ensureTargetDirectoryExists() {
		if (outputDirectory.exists()) {
			return true;
		}
		return outputDirectory.mkdirs();
	}

	public ImmutableList<String> gatherOcamlCompiledSources(final File root) {
		final ImmutableList.Builder<String> files = ImmutableList.builder();
		if (root.isFile() && isOcamlCompiledSourceFile(root)) {
			files.add(root.getPath());
			return files.build();
		}
	
		if (!root.isDirectory() || root.listFiles() == null)
			return files.build();
		
		for (final File file : root.listFiles()) {
			if (file.isDirectory()) {
				getLog().info("scanning directory: " + file);
				
				files.addAll(gatherOcamlCompiledSources(file));
			} else {
				if (isOcamlCompiledSourceFile(file)) {
					getLog().info("adding ocaml source file: " + file);
					files.add(file.getPath());
				}
			}
		}
		return files.build();
	}


	private boolean isOcamlCompiledSourceFile(final File file) {
		final String extension = getExtension(file.getAbsolutePath());
		return COMPILED_IMPL_EXTENSION.equalsIgnoreCase(extension);
		
	}

	public static String getExtension(final String filePath) {
		final int dotPos = filePath.lastIndexOf(DOT);
		if (-1 == dotPos) {
			return null;
		} else {
			return filePath.substring(dotPos+1);
		}
	}

	public String getTargetJar() {
		return project.getProperties().getProperty("jar.finalName");
	}
}
