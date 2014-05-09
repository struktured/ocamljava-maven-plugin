package mandelbrot.ocamljava_maven_plugin;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

public class OcamlConstants {

	public static final String IMPL_SOURCE_EXTENSION = "ml";
	public static final String INTERFACE_SOURCE_EXTENSION = "mli";

	public static final String DOT = ".";

	public static final String COMPILED_IMPL_EXTENSION = "cmj";
	public static final String COMPILED_INTERFACE_ENXTENSION = "cmi";
	public static final String OBJECT_BINARY_EXTENSION = "jo";

	public static final Set<String> OCAML_SOURCE_FILE_EXTENSIONS = ImmutableSet
			.of(IMPL_SOURCE_EXTENSION, INTERFACE_SOURCE_EXTENSION);
	
}
