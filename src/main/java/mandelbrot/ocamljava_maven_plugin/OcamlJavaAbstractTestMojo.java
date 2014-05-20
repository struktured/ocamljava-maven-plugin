package mandelbrot.ocamljava_maven_plugin;

import java.io.File;


public abstract class OcamlJavaAbstractTestMojo extends OcamlJavaAbstractMojo {


	/**
	 * The target test jar to add ocaml compiled sources to.
	 * @parameter default-value="${project.artifactId}-${project.version}-tests.jar"
	 * @required
	 * @readonly
	 */
	protected String targetTestJar;

	/**
	 * The target subfolder to hold all compiled ocaml test sources. This value
	 * is combined with the build output directory (usually <code>target</code>) to 
	 * create an actual file path. 
	 * 
	 * @parameter default-value="ocaml-tests"
	 * 
	 */
	protected String ocamlCompiledTestsTarget;
	
	/**
	 * Project's source directory as specified in the POM.
	 * 
	 * @parameter default-value="src/test/ocaml" 
	 * @readonly
	 */
	protected final File ocamlTestDirectory = new File("src/test/ocaml");
	
}
