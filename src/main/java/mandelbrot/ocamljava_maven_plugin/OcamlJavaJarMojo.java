package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.util.Collection;

import mandelbrot.ocamljava_maven_plugin.util.FilesByExtensionGatherer;
import mandelbrot.ocamljava_maven_plugin.util.JarAppender;
import ocaml.compilers.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * <p>This is a goal which attaches OCaml compiled sources to a target jar during the packaging phase.
 * It is the same as executing something like</p>
 * <p><code>ocamljava -o some-target.jar foo.cmj bar.cmj ...</code></p>
 * from the command line but instead uses maven properties to infer the compiled source location and target jar name.
 * Both can be overridden. See the configuration section of the documentation for more information.</p>
 * @requiresProject 
 * @goal jar
 * @phase package
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaJarMojo extends OcamlJavaAbstractMojo {


	/**
	 * The target jar to add ocaml compiled sources to.
	 * @parameter default-value="${project.artifactId}-${project.version}.jar"
	 * @required
	 * @readonly
	 */
	protected String targetJar;
	
	/***
	 * Determines whether to attach the compiled module interfaces to the final packaged jar.
	 * @parameter default-value="true"
	 */
	protected boolean attachCompiledModuleInterfaces;
	
	@Override
	public void execute() throws MojoExecutionException {

		if (!ensureTargetDirectoryExists()) {
			getLog().error("Could not create target directory");
			return;
		}
		
		try {

			final Multimap<String, String> ocamlCompiledSourceFiles = new FilesByExtensionGatherer(
					this, ImmutableSet.of(
							OcamlJavaConstants.COMPILED_IMPL_EXTENSION,
							OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION))
					.gather(new File(outputDirectory.getPath() + File.separator
							+ ocamlCompiledSourcesTarget));
		
			final String[] args = generateCommandLineArguments(targetJar, ocamlCompiledSourceFiles.get(
					OcamlJavaConstants.COMPILED_IMPL_EXTENSION)).toArray(new String[]{});

			getLog().info("args: " + ImmutableList.copyOf(args));
			ocamljavaMain.main(args);
			
			if (attachCompiledModuleInterfaces) {
				
				final Collection<String> compiledModuleInterfaces =
					ocamlCompiledSourceFiles.get(OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION);
				new JarAppender(this).addFiles(getTargetJarFullPath(targetJar), compiledModuleInterfaces);
			}
			
		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}

	
	private ImmutableList<String> generateCommandLineArguments(final String targetJar,
			final Collection<String> ocamlSourceFiles) {
		return ImmutableList.<String>builder().add(OcamlJavaConstants.ADD_TO_JAR_SOURCES_OPTION).
				add(getTargetJarFullPath(targetJar)).addAll(ocamlSourceFiles).build();
	}
	
	private boolean ensureTargetDirectoryExists() {
		if (outputDirectory.exists()) {
			return true;
		}
		return outputDirectory.mkdirs();
	}

	public String getTargetJarFullPath(final String targetJar) {
		return outputDirectory.getAbsolutePath() + File.separator + targetJar;
	}
}
