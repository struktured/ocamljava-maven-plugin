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

	public static final Set<String> OCAML_SOURCE_FILE_EXTENSIONS = ImmutableSet
			.of(IMPL_SOURCE_EXTENSION, INTERFACE_SOURCE_EXTENSION);
	
	public static final Set<String> OCAML_COMPILED_SOURCE_FILE_EXTENSIONS = ImmutableSet
			.of(COMPILED_IMPL_EXTENSION, COMPILED_INTERFACE_EXTENSION, OBJECT_BINARY_EXTENSION);
	
	public static final String COMPILE_SOURCES_OPTION = "-c";
	public static final String ADD_TO_JAR_SOURCES_OPTION = "-o";
	public static final String CLASSPATH_OPTION = "-classpath";
}
