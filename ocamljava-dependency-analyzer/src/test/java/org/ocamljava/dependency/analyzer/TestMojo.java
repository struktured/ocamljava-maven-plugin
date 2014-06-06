package org.ocamljava.dependency.analyzer;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public class TestMojo extends AbstractMojo {

	private static final TestMojo TEST_MOJO = new TestMojo();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
	}

	public static TestMojo instance() {
		return TEST_MOJO;
	}
}
 