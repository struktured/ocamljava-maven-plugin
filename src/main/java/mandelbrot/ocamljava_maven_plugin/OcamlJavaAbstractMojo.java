package mandelbrot.ocamljava_maven_plugin;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;


public abstract class OcamlJavaAbstractMojo extends AbstractMojo {
	/**
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;		

	/**
	 * The plugin descriptor.
	 * 
	 * @required
	 * @parameter default-value="${descriptor}"
	 */
	protected PluginDescriptor descriptor;	
	
	/**
	 * Project's output directory, usually <code>target</code>.
	 * 
	 * @parameter property="project.build.directory"
	 * @required
	 */
	protected final File outputDirectory = new File("");

	/**
	 * The target jar to add ocaml compiled sources to.
	 * @parameter default-value="${project.artifactId}-${project.version}.jar"
	 * @required
	 * @readonly
	 */
	protected String targetJar;

		
	/**
	 * The target subfolder to hold all compiled ocaml sources. This value
	 * is combined with the build output directory (usually <code>target</code>) to 
	 * create an actual file path. 
	 * 
	 * @parameter default-value="ocaml-bin"    
	 */
	protected String ocamlCompiledSourcesTarget;


	/**
	 * Project's source directory.
	 * 
	 * @parameter default-value="src/main/ocaml"
	 * @readonly
	 */
	protected final File ocamlSourceDirectory = new File("src/main/ocaml");

	
}
