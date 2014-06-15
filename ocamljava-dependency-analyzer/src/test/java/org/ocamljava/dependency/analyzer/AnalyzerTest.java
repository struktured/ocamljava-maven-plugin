package org.ocamljava.dependency.analyzer;

import static org.ocamljava.dependency.analyzer.SharedTestInstances.DEPENDABLE_MODULE;
import static org.ocamljava.dependency.analyzer.SharedTestInstances.DEPENDENT_MODULE;
import static org.ocamljava.dependency.analyzer.SharedTestInstances.DEPENDENT_MODULE2;
import static org.ocamljava.dependency.analyzer.SharedTestInstances.DEPENDENT_MODULE3;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;
import mandelbrot.dependency.analyzer.Analyzer;
import mandelbrot.dependency.analyzer.DependencyExtractor;
import mandelbrot.dependency.data.ModuleDescriptor;
import mandelbrot.dependency.data.ModuleKey.ModuleType;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;

public class AnalyzerTest {

	private static AbstractMojo testMojo;

	@Test
	public void shouldOrderTwoModulesByDependency() {


		final Set<String> sortedDependencies = SharedTestInstances.createSimpleDependency().keySet();

		final Iterator<String> iterator = sortedDependencies.iterator();

		verifyEntry(DEPENDABLE_MODULE, iterator);
		
		verifyEntry(DEPENDENT_MODULE, iterator);
	
	}


	private void verifyEntry(String modelName, final Iterator<String> iterator) {
		if (iterator.hasNext())
			Assert.assertEquals(iterator.next(), modelName);
		else
			Assert.fail("iterator should have next: " + iterator);
	}

	@Test
	public void shouldOrderThreeModulesByDependency() {

		final Analyzer analyzer = new Analyzer(testMojo);

		final ImmutableMultimap.Builder<String, Optional<String>> builder = ImmutableMultimap
				.builder();

		builder.put(DEPENDABLE_MODULE, Optional.<String> absent());

		builder.put(DEPENDENT_MODULE, Optional.of(DEPENDABLE_MODULE));

		builder.put(DEPENDENT_MODULE2, Optional.of(DEPENDABLE_MODULE));

		final DependencyExtractor dependencyExtractor = new DependencyExtractor(testMojo, false);
		
		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
				"dependable_module.ml", "dependent_module.ml",
				"dependent_module2.ml"));

		final Set<String> sortedDependencies = analyzer.sortDependenciesByModuleName(
				builder.build(), dependencyExtractor.getModuleToFilePath())
				.keySet();

		final Iterator<String> iterator = sortedDependencies.iterator();

		verifyEntry(DEPENDABLE_MODULE, iterator);
		verifyEntry(DEPENDENT_MODULE, iterator);
		verifyEntry(DEPENDENT_MODULE2, iterator);
	}

	@Test
	public void shouldOrderFourModulesByDependency() {

		final Analyzer analyzer = new Analyzer(testMojo);

		final ImmutableMultimap.Builder<String, Optional<String>> builder = ImmutableMultimap
				.builder();

		builder.put(DEPENDABLE_MODULE, Optional.<String> absent());

		builder.put(DEPENDENT_MODULE, Optional.of(DEPENDABLE_MODULE));
		builder.put(DEPENDENT_MODULE2, Optional.of(DEPENDENT_MODULE));
		builder.put(DEPENDENT_MODULE2, Optional.of(DEPENDABLE_MODULE));
		builder.put(DEPENDENT_MODULE3, Optional.of(DEPENDENT_MODULE));
		final DependencyExtractor dependencyExtractor = new DependencyExtractor(testMojo, false);
		
		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
				"dependable_module.ml", "dependent_module.ml",
				"dependent_module2.ml", "dependent_module3.ml"));
		
		final Set<String> sortedDependencies = analyzer.sortDependenciesByModuleName(
				builder.build(), dependencyExtractor.getModuleToFilePath())
				.keySet();

		final Iterator<String> iterator = sortedDependencies.iterator();


		verifyEntry(DEPENDABLE_MODULE, iterator);
		verifyEntry(DEPENDENT_MODULE, iterator);
		verifyEntry(DEPENDENT_MODULE3, iterator);
		verifyEntry(DEPENDENT_MODULE2, iterator);
		
	}

	@BeforeClass
	public static void beforeClass() {
		testMojo = new AbstractMojo() {

			@Override
			public void execute() throws MojoExecutionException,
					MojoFailureException {

			}
		};
	}

	@Before
	public void beforeTest() {
	}

	@After
	public void afterTest() {
	}

	@AfterClass
	public static void afterClass() {
		testMojo = null;
	}

	@Test
	public void shouldOrderTwoModulesInterfacesFirst() {

		final Analyzer analyzer = new Analyzer(testMojo);

		final ImmutableMultimap.Builder<String, Optional<String>> builder = ImmutableMultimap
				.builder();

		builder.put(DEPENDABLE_MODULE, Optional.<String> absent());

		builder.put(DEPENDENT_MODULE, Optional.of(DEPENDABLE_MODULE));

		final DependencyExtractor dependencyExtractor = SharedTestInstances.dependencyExtractor();
		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
				"dependable_module.ml", "dependable_module.mli", "dependent_module.ml", "dependent_module.mli"));

		final Set<Entry<String, ModuleDescriptor>> sortedDependencies = analyzer.sortDependenciesByModuleName(
				builder.build(), dependencyExtractor.getModuleToFilePath())
				.entries();

		final Iterator<Entry<String, ModuleDescriptor>> iterator = sortedDependencies.iterator();

		verifyEntry(iterator, DEPENDABLE_MODULE, ModuleType.INTERFACE);
		verifyEntry(iterator, DEPENDABLE_MODULE, ModuleType.IMPL);

		verifyEntry(iterator, DEPENDENT_MODULE, ModuleType.INTERFACE);
		verifyEntry(iterator, DEPENDENT_MODULE, ModuleType.IMPL);
		

	}

	private void verifyEntry(
			final Iterator<Entry<String, ModuleDescriptor>> iterator, final String prefix, final ModuleType moduleType ) {
		if (iterator.hasNext()) {
			final Entry<String, ModuleDescriptor> next = iterator.next();;
			
			Assert.assertEquals(next.getKey(), prefix);
			Assert.assertEquals(next.getValue().getModuleType(), moduleType);

			Assert.assertEquals(next.getValue().getModuleFile().get(), new File(prefix + "." + moduleType.getExtension()));
			
		}
		else
			Assert.fail("iterator should have next: " + iterator);
	}

}