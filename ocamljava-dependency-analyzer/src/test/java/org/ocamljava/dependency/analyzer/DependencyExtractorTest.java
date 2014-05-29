package org.ocamljava.dependency.analyzer;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class DependencyExtractorTest {

	private final AbstractMojo testMojo = new AbstractMojo() {
		
		@Override
		public void execute() throws MojoExecutionException, MojoFailureException {
			
		}
	};
	
	@Test
	public void shouldExtraOpenStatement() throws IOException {
		final String input = "open Foobar\nlet f = ()";
		
		final File dependent = File.createTempFile("foobar-dependent", ".ml");
		
		Multimap<String, String> groupSourcesByModuleDependencies = new DependencyExtractor(testMojo).groupSourcesByModuleDependencies(ImmutableList.of(dependent.getPath()));	

		System.out.println(groupSourcesByModuleDependencies);
	}
}
