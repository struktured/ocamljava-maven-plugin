package mandelbrot.ocamljava_compiler_maven_plugin;

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
 * @requiresProject
 * @goal compile
 * @phase compile
 * @executionStrategy once-per-session
 * @requiresDependencyResolution runtime
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaCompileMojo extends OcamlJavaCompileAbstractMojo {
	
	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

}
