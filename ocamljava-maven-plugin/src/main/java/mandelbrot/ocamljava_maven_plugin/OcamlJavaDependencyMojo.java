package mandelbrot.ocamljava_maven_plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * <p>
 * This is a goal which anaylzes OCaml sources during the maven process sources
 * phase to determine the build dependency order. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamldep -I com/foobar foo.ml bar.ml ...</code>
 * </p>
 * from the command line but instead uses maven properties to infer the source
 * locations and class path. All parameters can be overridden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @requiresProject
 * @goal dep
 * @phase process-sources
 * @executionStrategy once-per-session
 * @requiresDependencyResolution runtime
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaDependencyMojo extends OcamlJavaAbstractMojo {

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

}
