package mandelbrot.ocamljava_maven_plugin;

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ocamljava.runtime.wrappers.OCamlWrappers;
import org.ocamljava.wrapper.Ocamlwrap;


/**
 * <p>This is a goal which wraps OCaml compiled source interfaces with code generated java equivalents.
 * It is the same as executing something like</p>
 * <p><code>ocamlwrap lib.cmi</code></p>
 * from the command line but instead uses maven properties to infer the compiled module interface locations.
 * All parameters can be overriden. See the configuration section of the documentation for more information.</p>
 * @requiresProject 
 * @goal wrap
 * @phase generate-sources
 * @threadSafe *
 * @since 1.0
 */
public class OcamlWrapMojo extends OcamlJavaAbstractMojo {

	/***
	 * Prefix for names of generated classes. Default value is blank.
	 * 
	 * @parameter default-value=""
	 */
	protected String classNamePrefix = "";


	/***
	 * Suffix for names of generated classes.
	 * 
	 * @parameter default-value="Wrapper"
	 */
	protected String classNameSuffix = "Wrapper";

	
	/***
	 * Arguments passed for library initialization. Defaults to empty.
	 * @parameter 
	 */
	protected String[] libraryArgs = new String[] {};
	
			
	public static enum LibraryInitMode {
		STATIC,
		EXPLICIT;
		
		public String toCommandLineValue() { return name().toLowerCase(); }
	}
	
	/**
	 * Library initialization mode. One of <code>EXPLICIT</code> or <code>STATIC</code>. 
	 * 
	 * @parameter default-value="EXPLICIT"
	 */
	protected LibraryInitMode libraryInitMode = LibraryInitMode.EXPLICIT;
    
	/***
	 * Library package.
	 * 
	 * @parameter default-value=""
	 */
	protected String libaryPackage = "";

	/***
	 * Whether to disable warnings.
	 *
	 * @parameter default-value="false"
	 */
	protected boolean noWarnings;
	
	//-package <string>	""	package of generated classes
	
	public static enum StringMapping {
		JAVA_STRING,
		OCAMLSTRING,
		BYTE_ARRAY;
		public String toCommmandLineArgument() {
			return name().toLowerCase().replace("_", "-");
		}
	}
	
	/***
	 * Determines the string mapping for OCaml string type. 
	 * One of <code>JAVA_STRING</code>, <code>OCAMLSTRING</code>, or <code>BYTE-ARRAY</code>.
	 * 
	 * @parameter default-value="JAVA_STRING"
	 */
	protected StringMapping stringMapping = StringMapping.JAVA_STRING;
	
	
	/***
	 * Whether to enable verbose mode.
	 *	 
	 * @parameter default-value="false"
	 *
	 **/
	protected boolean verbose;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<String> modules = null;
		org.ocamljava.wrapper.ocamljavaMain.main(generateCommandLineArguments(modules));
	}

	private String[] generateCommandLineArguments(List<String> modules) {
		// TODO Auto-generated method stub
		return null;
	}

}
