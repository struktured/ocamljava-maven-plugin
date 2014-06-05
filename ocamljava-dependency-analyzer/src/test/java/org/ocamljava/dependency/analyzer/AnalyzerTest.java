package org.ocamljava.dependency.analyzer;

import java.io.File;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.ocamljava.dependency.data.ModuleDescriptor;
import org.ocamljava.dependency.data.ModuleKey.ModuleType;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;

public class AnalyzerTest {

	private static final String DEPENDENT_MODULE3 = "dependent_module3";
	private static final String DEPENDENT_MODULE2 = "dependent_module2";
	private static final String DEPENDABLE_MODULE = "dependable_module";
	private static final String DEPENDENT_MODULE = "dependent_module";
	private static AbstractMojo testMojo;

	@Test
	public void shouldOrderTwoModulesByDependency() {

		final Analyzer analyzer = new Analyzer(testMojo);

		final ImmutableMultimap.Builder<String, Optional<String>> builder = ImmutableMultimap
				.builder();

		builder.put(DEPENDABLE_MODULE, Optional.<String> absent());

		builder.put(DEPENDENT_MODULE, Optional.of(DEPENDABLE_MODULE));

		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
				"dependable_module.ml", "dependent_module.ml"));

		final Set<String> sortedDependencies = analyzer.sortDependencies(
				builder.build(), dependencyExtractor.getModuleToFilePath())
				.keySet();

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

		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
				"dependable_module.ml", "dependent_module.ml",
				"dependent_module2.ml"));

		final Set<String> sortedDependencies = analyzer.sortDependencies(
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

		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
				"dependable_module.ml", "dependent_module.ml",
				"dependent_module2.ml", "dependent_module3.ml"));

		final Set<String> sortedDependencies = analyzer.sortDependencies(
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

	private DependencyExtractor dependencyExtractor;

	@Before
	public void beforeTest() {
		this.dependencyExtractor = new DependencyExtractor(testMojo, false);
	}

	@After
	public void afterTest() {
		this.dependencyExtractor = null;
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

		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
				"dependable_module.ml", "dependable_module.mli", "dependent_module.ml", "dependent_module.mli"));

		final Set<Entry<String, ModuleDescriptor>> sortedDependencies = analyzer.sortDependencies(
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