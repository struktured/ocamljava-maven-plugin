package org.ocamljava.dependency.analyzer;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;
import org.ocamljava.dependency.data.ModuleDescriptor;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

/**  
* Given we know module X requires modules Y,Z, etc.,
* this class finds an ordering of the modules such that X < Y iff Y requires X.
* We can also assume that if D = {Y_0, Y_1, ..., Y_N} and X < D, Y < D,
* then X < Y iff string(X) < string(Y)
* X < X is false and of course, X = X.
*/
public class Analyzer {
	private final AbstractMojo abstractMojo;
	private final DependencyExtractor dependencyExtractor;
	
	public Analyzer(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
		this.dependencyExtractor = new DependencyExtractor(this.abstractMojo);
	}

	public static Optional<String> moduleNameOfSource(final String source) {
		if (StringUtils.isBlank(source))
			return Optional.absent();

		final String name = new File(source).getName();
		final String extension = org.codehaus.plexus.util.FileUtils
				.getExtension(name);

		final String rawModuleName = name.substring(0, name.length()
				- (extension.length() == 0 ? 0 : extension.length()
						+ File.separator.length()));

		final String lowerCasedName = rawModuleName.toLowerCase();

		if (lowerCasedName.isEmpty())
			return Optional.absent();

		return Optional.of(lowerCasedName);

	}
	public SortedSetMultimap<String, ModuleDescriptor> resolveModuleDependencies(
			final Collection<String> sources) {
		return resolveModuleDependencies(sources, Paths.get("").toFile());
	}
	
	public SortedSetMultimap<String, ModuleDescriptor> resolveModuleDependencies(
			final Collection<String> sources, final File prefixToTruncate) {
		final Multimap<String, Optional<String>> sourcesByModuleDependencies = 
				dependencyExtractor.groupSourcesByModuleDependencies(sources, prefixToTruncate);

		return sortDependencies(sourcesByModuleDependencies, dependencyExtractor.getModuleToFilePath());

	}

	
	public SortedSetMultimap<String, ModuleDescriptor> sortDependencies(final Multimap<String, Optional<String>> sourcesByModuleDependencies,
			final SortedSetMultimap<String, ModuleDescriptor> moduleToFilePath) {
	
		final Multimap<String, Optional<String>> modulesByModuleDependencies = Multimaps
				.transformEntries(sourcesByModuleDependencies,
						new Maps.EntryTransformer<String, Optional<String>, Optional<String>>() {

				@Override
				public Optional<String> transformEntry(final String key, final Optional<String> source) {
								if (source.isPresent())
								{
									final Optional<String> moduleNameOfSource = moduleNameOfSource(source.get());
									return moduleNameOfSource;
								} else {
									return Optional.absent();
								}
							}
						});
		
			final TreeMultimap<String, ModuleDescriptor> treeMultimap = 
					TreeMultimap.create(createComparator(modulesByModuleDependencies),
					moduleToFilePath.valueComparator()
			);
			
			final boolean changed = treeMultimap.putAll(moduleToFilePath);
			Preconditions.checkState(changed);
			
			return Multimaps.unmodifiableSortedSetMultimap(treeMultimap);
	}

	private static final Comparator<String> createComparator(
			final Multimap<String, Optional<String>> dependencies) {

		return new Comparator<String>() {

			@Override
			public int compare(final String module1, final String module2) {

				if (Objects.equal(module1, module2))
					return 0;

				if (StringUtils.isBlank(module1)
						&& !StringUtils.isBlank(module2))
					return -1;

				if (StringUtils.isBlank(module2)
						&& !StringUtils.isBlank(module1))
					return 1;

				final Collection<Optional<String>> module1Dependencies = dependencies
						.get(module1);
				final Collection<Optional<String>> module2Dependencies = dependencies
						.get(module2);

				final boolean module1RequiresModule2 = module1Dependencies
						.contains(Optional.of(module2));
				final boolean module2RequiresModule1 = module2Dependencies
						.contains(Optional.of(module1));

				if (module2RequiresModule1 && module1RequiresModule2) {
					throw new IllegalStateException("circular dependency: "
							+ module1 + " and " + module2);
				}

				// The dependable is less than the dependent.
				if (module2RequiresModule1)
					return -1;
				if (module1RequiresModule2)
					return 1;

				// Choose one with smallest number of dependencies
				final int cmp = Long.compare(module1Dependencies.size(),
						module2Dependencies.size());
				if (cmp != 0)
					return cmp;

				// When all else fails use default alphanumeric comparison
				return module1.compareTo(module2);

			}
		};
	}

}
