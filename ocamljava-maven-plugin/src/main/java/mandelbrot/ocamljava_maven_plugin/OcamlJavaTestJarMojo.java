package mandelbrot.ocamljava_maven_plugin;

import static mandelbrot.ocamljava_maven_plugin.OcamlJavaConstants.COMPILED_IMPL_EXTENSION;

import java.io.File;

import ocaml.compilers.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;

import com.google.common.collect.ImmutableList;

/**
 * <p>This is a goal which attaches OCaml compiled sources to a target jar during the packaging phase.
 * It is the same as executing something like</p>
 * <p><code>ocamljava -o some-target.jar foo.cmj bar.cmj ...</code></p>
 * from the command line but instead uses maven properties to infer the compiled source location and target jar name.
 * Both can be overriden. See the configuration section of the documentation for more information.</p>
 * @requiresProject 
 * @goal test-jar
 * @phase package
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaTestJarMojo extends OcamlJavaJarAbstractMojo {



	@Override
	public void execute() throws MojoExecutionException {

		if (!ensureTargetDirectoryExists()) {
			getLog().error("Could not create target directory");
			return;
		}
		
		try {
			
			final ImmutableList<String> ocamlTestFiles = gatherOcamlCompiledSources(new File(
					outputDirectory.getPath() + 
					File.separator + 
					ocamlCompiledTestsTarget));
		
			final String[] testArgs = generateCommandLineArguments(targetTestJar, ocamlTestFiles).toArray(new String[]{});

			getLog().info("test args: " + ImmutableList.copyOf(testArgs));
			ocamljavaMain.main(testArgs);

		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}

	
	private ImmutableList<String> generateCommandLineArguments(final String targetJar,
			final ImmutableList<String> ocamlSourceFiles) {
		return ImmutableList.<String>builder().add(OcamlJavaConstants.ADD_TO_JAR_SOURCES_OPTION).add(getTargetJarFullPath(targetJar)).addAll(ocamlSourceFiles).build();
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
	
		final File[] listFiles; 
		if (!root.isDirectory() || (listFiles = root.listFiles()) == null)
			return files.build();
		
		for (final File file : listFiles) {
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
		final String extension = FileUtils.getExtension(file.getPath());
		return COMPILED_IMPL_EXTENSION.equalsIgnoreCase(extension);
	}

	public String getTargetJarFullPath(final String targetJar) {
		return outputDirectory.getPath() + File.separator  + targetJar;
	}


	@Override
	protected String chooseTargetJar() {
		return targetTestJar;
	}
	
	@Override
	protected String chooseTargetOcamlJar() {
		return targetTestOcamlJar;
	}
}
