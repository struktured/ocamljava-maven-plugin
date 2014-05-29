package org.ocamljava.dependency.analyzer;

import java.util.Iterator;
import java.util.SortedSet;

import junit.framework.Assert;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

import com.google.common.collect.ImmutableMultimap;


public class AnalyzerTest {


	@Test
	public void shouldOrderTwoModulesByDependency() {
		
		final Analyzer analyzer = new Analyzer(testMojo);
	
		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
	
		builder.put("dependableModule", "");
		
		builder.put("dependentModule", "dependableModule");
		
		final SortedSet<String> sortedDependencies = analyzer.sortDependencies(builder.build());
		
		final Iterator<String> iterator = sortedDependencies.iterator();
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependableModule");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependentModule");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		
	}

	@Test
	public void shouldOrderThreeModulesByDependency() {
		
		final Analyzer analyzer = new Analyzer(testMojo);
	
		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
	
		builder.put("dependableModule", "");
		
		builder.put("dependentModule", "dependableModule");
		
		builder.put("dependentModule2", "dependableModule");
		
		final SortedSet<String> sortedDependencies = analyzer.sortDependencies(builder.build());
		
		final Iterator<String> iterator = sortedDependencies.iterator();
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependableModule");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependentModule");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependentModule2");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		
	}


	@Test
	public void shouldOrderFourModulesByDependency() {
		
		final Analyzer analyzer = new Analyzer(testMojo);
	
		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap.builder();
	
		builder.put("dependableModule", "");
		
		builder.put("dependentModule", "dependableModule");
		
		builder.put("dependentModule2", "dependentModule");
		builder.put("dependentModule2", "dependableModule");
		builder.put("dependentModule3", "dependentModule");
		
		
		final SortedSet<String> sortedDependencies = analyzer.sortDependencies(builder.build());
		
		final Iterator<String> iterator = sortedDependencies.iterator();
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependableModule");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependentModule");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependentModule3");
		else
			Assert.fail("iterator should have next: " + iterator);
		
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), "dependentModule2");
		else
			Assert.fail("iterator should have next: " + iterator);
		
	}

	private final AbstractMojo testMojo = new AbstractMojo() {
		
		@Override
		public void execute() throws MojoExecutionException, MojoFailureException {
			
		}
	};
	
	
}