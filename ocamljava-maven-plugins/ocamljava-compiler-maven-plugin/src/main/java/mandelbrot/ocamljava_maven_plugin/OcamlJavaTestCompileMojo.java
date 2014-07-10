package mandelbrot.ocamljava_maven_plugin;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * <p>
 * This is a goal which compiles OCaml test sources during the maven test
 * compilation phase. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamljava -classpath classpath/lib.jar -c foo-test.ml bar-test.ml ...</code>
 * </p>
 * from the command line but instead uses maven properties to infer the source
 * locations and class path. All parameters can be overriden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @since 1.0
 */
@Mojo(requiresProject=true, threadSafe=true, requiresDependencyResolution = ResolutionScope.RUNTIME, 
executionStrategy="once-per-session", defaultPhase=LifecyclePhase.TEST_COMPILE, name="testCompile")
public class OcamlJavaTestCompileMojo extends OcamlJavaCompileAbstractMojo {

	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlTestDirectory;
	}
	
	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledTestsTarget;
	}
	
	@Override
	public String fullyQualifiedGoal() {
		return "mandelbrot:ocamljava-compiler-maven-plugin:testCompile";
	}	
}
