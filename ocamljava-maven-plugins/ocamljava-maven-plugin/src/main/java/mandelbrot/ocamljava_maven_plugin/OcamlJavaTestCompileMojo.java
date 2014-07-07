package mandelbrot.ocamljava_maven_plugin;

import java.io.File;

import mandelbrot.ocamljava_compiler_maven_plugin.OcamlJavaCompileAbstractMojo;

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
 * @requiresProject
 * @goal testCompile
 * @phase test-compile
 * @executionStrategy once-per-session
 * @requiresDependencyResolution runtime
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaTestCompileMojo extends OcamlJavaCompileAbstractMojo {

	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlTestDirectory;
	}
	
	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledTestsTarget;
	}
	
}
