package org.ocamljava.dependency.analyzer;

import org.apache.maven.plugin.AbstractMojo;
import org.ocamljava.dependency.data.ModuleDescriptor;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.SortedSetMultimap;

public class SharedTestInstances {

	public static final String DEPENDENT_MODULE3 = "dependent_module3";
	public static final String DEPENDENT_MODULE2 = "dependent_module2";
	public static final String DEPENDABLE_MODULE = "dependable_module";
	public static final String DEPENDENT_MODULE = "dependent_module";
	public static final String COM_SECOND = "com.asecond";
	public static final String COM_FIRST = "com.zfirst";

	private static final AbstractMojo testMojo = TestMojo.instance();
	static final Analyzer analyzer = new Analyzer(testMojo);
	 
	public static SortedSetMultimap<String, ModuleDescriptor> createSimpleDependency() {
		final ImmutableMultimap.Builder<String, Optional<String>> builder = ImmutableMultimap
				.builder();

		builder.put(DEPENDABLE_MODULE, Optional.<String> absent());

		builder.put(DEPENDENT_MODULE, Optional.of(DEPENDABLE_MODULE));

		final DependencyExtractor dependencyExtractor = dependencyExtractor();
	
		dependencyExtractor.groupSourcesByModuleDependencies(ImmutableList.of(
			  "com/zfirst/dependable_module.ml", "com/asecond/dependent_module.ml"));

		final SortedSetMultimap<String, ModuleDescriptor> sortedDependencies = analyzer.sortDependencies(
				builder.build(), dependencyExtractor.getModuleToFilePath());
				
		return sortedDependencies;
	}

	public static DependencyExtractor dependencyExtractor() {
		return new DependencyExtractor(testMojo, false);
	}

}
