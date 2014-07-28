package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;

import org.codehaus.plexus.util.FileUtils;
import com.google.common.base.*;

public class FilePredicates {

	public static Predicate<String> exists() 
	{
		return new Predicate<String>() { @Override public boolean apply(final String input) { return new File(input).exists(); } };
	}
}
