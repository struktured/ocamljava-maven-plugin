package mandelbrot.ocamljava_maven_plugin;

import java.io.File;

import mandelbrot.ocamljava_maven_plugin.util.FileMappings;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;


public abstract class OcamlJavaAbstractMojo extends AbstractMojo {
	
	
	/***
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;		

	/***
	 * The plugin descriptor.
	 * 
	 * @required
	 * @parameter default-value="${descriptor}"
	 */
	protected PluginDescriptor descriptor;	
	
	/***
	 * Project's output directory, usually <code>target</code>.
	 * 
	 * @parameter property="project.build.directory"
	 * @required
	 */
	protected final File outputDirectory = new File("");

		
	/***
	 * The target subfolder to hold all compiled ocaml sources. This value
	 * is combined with the build output directory (usually <code>target</code>) to 
	 * create an actual file path. 
	 * 
	 * @parameter default-value="ocaml-bin"    
	 */
	protected String ocamlCompiledSourcesTarget;


	/***
	 * Project's source directory.
	 * 
	 * @parameter default-value="src/main/ocaml"
	 */
	protected File ocamlSourceDirectory = new File("src/main/ocaml");

	/***
	 * The target jar to depend on and possibly replace with ocaml compiled sources depending on
	 * the value of the <code>replaceMainArtifact</code> parameter. 
	 * @parameter default-value="${project.artifactId}-${project.version}.jar"
	 * @required
	 */
	protected String targetJar;

	/***
	 * The target jar created by the ocamljava jar creation tool. If <code>replaceMainArtifact</code> is
	 * set to <code>true</code>, then this jar will replace the contents of the <code>targetJar</code> parameter.
	 * @parameter default-value="${project.artifactId}-${project.version}-ocaml.jar"
	 * @required
	 */
	protected String targetOcamlJar;

	public String getOcamlCompiledSourcesTargetFullPath() {
		return outputDirectory.getPath() + File.separator
				+ ocamlCompiledSourcesTarget;
	}

	/***
	 * The target test jar to depend on and possibly replace with ocaml compiled sources depending on
	 * the value of the <code>replaceMainArtifact</code> parameter. 
	 * @parameter default-value="${project.artifactId}-${project.version}-tests.jar"
	 * @required
	 */
	protected String targetTestJar;

	/***
	 * The target test jar created by the ocamljava jar creation tool. If <code>replaceMainArtifact</code> is
	 * set to <code>true</code>, then this jar will replace the contents of the <code>targetTestJar</code> parameter.
	 * @parameter default-value="${project.artifactId}-${project.version}-ocaml-tests.jar"
	 * @required
	 */
	protected String targetTestOcamlJar;

	/***
	 * The target subfolder to hold all compiled ocaml test sources. This value
	 * is combined with the build output directory (usually <code>target</code>) to 
	 * create an actual file path. 
	 * 
	 * @parameter default-value="ocaml-tests"
	 * 
	 */
	protected String ocamlCompiledTestsTarget;
	
	/***
	 * Project's source directory as specified in the POM.
	 * 
	 * @parameter default-value="src/test/ocaml" 
	 */
	protected final File ocamlTestDirectory = new File("src/test/ocaml");
	
	/***
	 * Sets how java packages are determined for the code generated classes.
	 * <p>By default, a java package will be inferred according to the folder structure of the modules.
	 * For instance, <code>"src/main/ocaml/foo/bar/lib.ml"</code> will generate <code>package foo.bar</code> at the top of <code>LibWrapper.java</code>.
	 * To fix the package name for all compiled module interfaces, set this value to <code>FIXED</code> and fill in the {@link #packageName} parameter
	 * accordingly.</p>
	 *
	 * @parameter default-value="DYNAMIC"
	 * 
	 **/	
	protected JavaPackageMode javaPackageMode = JavaPackageMode.DYNAMIC;

	
	public JavaPackageMode getJavaPackageMode() {
		return javaPackageMode;
	}

	public void setJavaPackageMode(JavaPackageMode javaPackageMode) {
		this.javaPackageMode = javaPackageMode;
	}

	public static enum JavaPackageMode {
		FIXED,
		DYNAMIC
	}

	/***
	 * Sets the java package name for each source file.
	 * 
	 * @parameter default-value=""
	 * 
	 **/	
	protected String packageName;
	
	protected String toPackage(final File prefixToTruncate, final String path) {
		
		if (JavaPackageMode.DYNAMIC.equals(javaPackageMode)) {
		return FileMappings.toPackage(prefixToTruncate, path);
		} else {
			return packageName;
		}
	}
}
