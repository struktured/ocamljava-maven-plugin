package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;

public class FileExtensions {

	private static final String DOT = ".";

	public static String extensionOf(final String value) {
		return FileUtils.getExtension(value);
	}
	
	public static String changeExtension(final File srcFile, final String extension) {
		return srcFile.getName().split("\\" + DOT)[0] + DOT + extension;
	}
}
