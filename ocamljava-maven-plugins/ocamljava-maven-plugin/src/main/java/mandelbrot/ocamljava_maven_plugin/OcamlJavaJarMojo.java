package mandelbrot.ocamljava_maven_plugin;

import java.io.File;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;


/**
 * <p>This is a goal which attaches OCaml compiled sources to a target jar during the packaging phase.
 * It is the same as executing something like</p>
 * <p><code>ocamljava -o some-target.jar foo.cmj bar.cmj ...</code></p>
 * from the command line but instead uses maven properties to infer the compiled source location and target jar name.
 * Both can be overridden. See the configuration section of the documentation for more information.</p>
 * @since 1.0
 */
@Mojo(name="jar", requiresProject=true, defaultPhase=LifecyclePhase.PACKAGE, threadSafe=true)
public class OcamlJavaJarMojo extends OcamlJavaJarAbstractMojo {

	@Override
	protected String chooseTargetJar() {
		return targetJar;
	}

	@Override
	protected String chooseTargetOcamlJar() {
		return targetOcamlJar;
	}

	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}


	
}
