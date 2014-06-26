package mandelbrot.ocamljava_maven_plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;

import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.tools.ocamldep.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ocamljava.runtime.annotations.parameters.Parameters;
import org.ocamljava.runtime.parameters.NativeParameters;

import com.google.common.collect.ImmutableList;

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
		
		generateDependencyGraph();
		
	}

	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

}
