package mandelbrot.ocamljava_maven_plugin;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;

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
@Mojo(requiresProject=true, threadSafe=true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
executionStrategy="once-per-session", defaultPhase=LifecyclePhase.COMPILE, name="compile")
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
	public String fullyQualifiedGoal() {
		return "mandelbrot:ocamljava-compiler-maven-plugin:compile";
	}

}
