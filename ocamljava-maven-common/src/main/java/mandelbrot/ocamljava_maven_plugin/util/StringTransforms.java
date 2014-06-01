package mandelbrot.ocamljava_maven_plugin.util;

import com.google.common.base.Preconditions;

public class StringTransforms {

	/***
	 * Removes white space, as well as <code>toTrim</code> from the beginning and end of the input string. For instance,
	 * the string <code>" 111hello11 "</code> would become <code>"hello"</code> if <code>toTrim="1"</code>.
	 * @param input the input string to trim.
	 * @param toTrim the trim sequence. 
	 * @return a trimmed string, or <code>null</code> for null <code>input</code>.
	 * @throws NullPointerException for <code>null toTrim</code>
	 */
	public static String trim(String input, final String toTrim) {
		
		if (input == null)
			return null;
		
		Preconditions.checkNotNull(toTrim);
	
		input = input.trim();
		
		while (!input.isEmpty() && input.length() >= toTrim.length()
				&& input.startsWith(toTrim))
			input = input.substring(toTrim.length(), input.length());

		while (!input.isEmpty() && input.length() >= toTrim.length()
				&& input.endsWith(toTrim))
			input = input.substring(0, input.length() - toTrim.length());		
		
		return input;
	}


	/***
	 * Removes white space, as well as <code>toTrim</code> from the beginning and end of the input string. 
	 * Equivalent to invoking <code>trim(input, String.valueOf(toTrim))</code>.
	 * @param input the input string to trim.
	 * @param toTrim the trim character. 
	 * @return a trimmed string, or <code>null</code> for null <code>input</code>.
	 */
	public static String trim(final String input, final char toTrim) {
		return trim(input, String.valueOf(toTrim));
	}
}
