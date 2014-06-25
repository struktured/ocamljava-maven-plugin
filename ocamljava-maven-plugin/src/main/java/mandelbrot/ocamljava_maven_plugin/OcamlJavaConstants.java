package mandelbrot.ocamljava_maven_plugin;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/***
 * Various constants specific to the ocamljava compiler library.
 * @author Carmelo Piccione
 *
 */
public class OcamlJavaConstants {

	public static final String IMPL_SOURCE_EXTENSION = "ml";
	public static final String INTERFACE_SOURCE_EXTENSION = "mli";

	public static final String DOT = ".";

	public static final String COMPILED_IMPL_EXTENSION = "cmj";
	public static final String COMPILED_INTERFACE_EXTENSION = "cmi";
	public static final String OBJECT_BINARY_EXTENSION = "jo";
	public static final String JSON_EXTENSION = "json";
	
	public static final Set<String> OCAML_SOURCE_FILE_EXTENSIONS = ImmutableSet
			.of(IMPL_SOURCE_EXTENSION, INTERFACE_SOURCE_EXTENSION);
	
	public static final Set<String> OCAML_COMPILED_SOURCE_FILE_EXTENSIONS = ImmutableSet
			.of(COMPILED_IMPL_EXTENSION, COMPILED_INTERFACE_EXTENSION, OBJECT_BINARY_EXTENSION);
	
	public static final String COMPILE_SOURCES_OPTION = "-c";
	public static final String ADD_TO_JAR_SOURCES_OPTION = "-o";
	public static final String RECORD_DEBUGGING_INFO_OPTION = "-g";
	public static final String ADDITIONAL_JAR_OPTION = "-additional-jar";
	public static final String CLASSPATH_OPTION = "-classpath";
	
	
//	Usage: ocamlwrap.jar <options> <files>
//	Options are:
//	  -class-name-prefix <string>  Set prefix for class names
//	  -class-name-suffix <string>  Set suffix for class names
//	  -I <string>  Add to search path
//	  -library-args <string>  Arguments passed for library initialization
//	  -library-init {explicit|static}  Set initialization mode
//	  -library-package <string>  Set library package
//	  -no-warnings  Disable warnings
//	  -package <string>  Set package name
//	  -string-mapping {java-string|ocamlstring|byte-array}  Set mapping for strings
//	  -verbose  Enable verbose mode
//	  -help  Display this list of options
//	  --help  Display this list of options

	public static final String LIBRARY_PACKAGE_OPTION = "-library-package";
	public static final String LIBRARY_INIT_OPTION = "-library-init";
	public static final String LIBRARY_ARGS_OPTION = "-library-args";
	public static final String STRING_MAPPING_OPTION = "-string-mapping";
	public static final String VERBOSE_OPTION = "-verbose";
	public static final String NO_WARNINGS_OPTION = "-no-warnings";
	public static final String CLASS_NAME_PREFIX_OPTION = "-class-name-prefix";
	public static final String CLASS_NAME_SUFFIX_OPTION = "-class-name-suffix";
	public static final String PACKAGE_OPTION = "-package";
	public static final String JAVA_PACKAGE_OPTION = "-java-package";
	public static final String INCLUDE_DIR_OPTION = "-I";
	public static final String COMPACT_OPTION = "-compact";
	public static final String JAVA_EXTENSIONS_OPTION = "-java-extensions";
	public static final String JAVA_ONLY_OPTION = "-java";
	public static final String SORT_OPTION = "-sort";
	public static final String ALL_OPTION = "-all";
	
}
