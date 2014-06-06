package org.ocamljava.dependency.analyzer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;

public class DependencyExtractorTest {

	private final AbstractMojo testMojo = new AbstractMojo() {
		
		@Override
		public void execute() throws MojoExecutionException, MojoFailureException {
			
		}
	};
	
	
	private File dependable;
	private File dependent;
	
	@Test
	public void shouldExtractOpenStatement() throws IOException {
		final String input = "open Foobar\n" +
					"let f = ()";
		
		final File dependent = new File("foobar-dependent.ml");
		
		final String input2 = "type color = BLACK|WHITE";
		final File dependable = new File("foobar.ml");
	
		writeData(input, dependent);	
		writeData(input2, dependable);	
		
		final List<String> list = ImmutableList.of(dependent.getPath(), dependable.getPath());
		
		final Multimap<String, Optional<String>> groupSourcesByModuleDependencies = new DependencyExtractor(testMojo).
				groupSourcesByModuleDependencies(list);	
		//System.out.println(groupSourcesByModuleDependencies);
		
		final Collection<Optional<String>> collection = groupSourcesByModuleDependencies.get("foobar");
		
		Assert.assertEquals(1, collection.size());
		Assert.assertEquals(collection.iterator().next(), Optional.absent());

		final Collection<Optional<String>> collection2 = groupSourcesByModuleDependencies.get("foobar-dependent");
		
		Assert.assertEquals(2, collection2.size());
		Assert.assertTrue(collection2.contains(Optional.of("foobar")));
		Assert.assertTrue(collection2.contains(Optional.absent()));
		

	}

	private void writeData(final String input, final File dependent)
			throws IOException {
		final FileWriter fileWriter = new FileWriter(dependent);
		
		try {
			fileWriter.write(input);		
		} 
		finally { 
		fileWriter.close();
		}
	}

	@After
	public void afterTests() {
		

		if (dependable != null)
		FileUtils.deleteQuietly(dependable);
		
		if (dependent != null)
			FileUtils.deleteQuietly(dependent);

	}
	
	@Test
	public void shouldExtractScopedModule() throws IOException {
		final String input = "let z = Foobar.BLACK\n";
		
		this.dependent = new File("foobar-dependent.ml");
		
		final String input2 = "type color = BLACK|WHITE";
		this.dependable = new File("foobar.ml");
	
		writeData(input, dependent);	
		writeData(input2, dependable);	
		
		final List<String> list = ImmutableList.of(dependent.getPath(), dependable.getPath());
		
		final Multimap<String, Optional<String>> groupSourcesByModuleDependencies = new DependencyExtractor(testMojo).
				groupSourcesByModuleDependencies(list);	
		
		final Collection<Optional<String>> collection = groupSourcesByModuleDependencies.get("foobar");
		
		Assert.assertEquals(1, collection.size());
		Assert.assertEquals(collection.iterator().next(), Optional.absent());

		final Collection<Optional<String>> collection2 = groupSourcesByModuleDependencies.get("foobar-dependent");
		
		Assert.assertTrue(collection2.contains(Optional.of("foobar")));
		Assert.assertTrue(collection2.contains(Optional.absent()));
		

	}
	
}