package mandelbrot.ocamljava_maven_plugin.util;

import junit.framework.Assert;

import org.junit.Test;

public class StringTransformsTest {

	@Test
	public void shouldTrimWhiteSpaceAndPattern() {
		final String input = " 111hello11 ";
		
		final String trimmed = StringTransforms.trim(input, "1");
	
		Assert.assertEquals("hello", trimmed);
		
	}

	@Test
	public void shouldTrimWhiteSpace() {
		final String input = " 111hello11 ";
		
		final String trimmed = StringTransforms.trim(input, "2");
	
		Assert.assertEquals("111hello11", trimmed);
		
	}
	
	@Test
	public void shouldHandleBlankString() {
		final String input = "";
		
		final String trimmed = StringTransforms.trim(input, "");
	
		Assert.assertTrue(trimmed.isEmpty());
		
	}
}
