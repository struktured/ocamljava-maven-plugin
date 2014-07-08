package mandelbrot.ocamljava_maven_plugin;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import mandelbrot.ocamljava_compiler_maven_plugin.OcamlJavaCompileAbstractMojo;

/**
 * <p>
 * This is a goal which compiles OCaml sources during the maven compilation
 * phase. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamljava -classpath classpath/lib.jar -c foo.ml bar.ml ...</code>
 * </p>
 * from the command line but instead uses maven properties to infer the source
 * locations and class path. All parameters can be overridden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @since 1.0
 */
@Mojo(requiresProject=true, requiresDependencyResolution=ResolutionScope.RUNTIME, executionStrategy="once-per-session",
threadSafe=true, defaultPhase=LifecyclePhase.COMPILE, name="compile")
public class OcamlJavaCompileMojo extends OcamlJavaCompileAbstractMojo {
	
	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		invokePlugin("mandelbrot:ocamljava-compiler-maven-plugin:compile", false);
	}
}
