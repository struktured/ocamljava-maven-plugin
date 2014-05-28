package org.ocamljava.dependency.analyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Comparator;
import java.util.Scanner;
import java.util.SortedSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

public class Analyzer {

	private static final String MODULE_REGEX_START_OF_LINE = "[A-Z][a-zA-z0-9_]+\\.";
	private static final String MODULE_REGEX_MIDDLE_OF_LINE = "[\\s\\,\\;\\=\\+]"
			+ MODULE_REGEX_START_OF_LINE;

	final Pattern PATTERN_MODULE_MIDDLE_OF_LINE = Pattern
			.compile(MODULE_REGEX_MIDDLE_OF_LINE);
	final Pattern PATTERN_MODULE_START_OF_LINE = Pattern
			.compile(MODULE_REGEX_START_OF_LINE);

	private final AbstractMojo abstractMojo;

	public Analyzer(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
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

		final String casedName = StringUtils
				.capitalizeFirstLetter(rawModuleName);

		if (casedName.isEmpty())
			return Optional.absent();

		return Optional.of(casedName);

	}

	public SortedSet<String> resolveModuleDependencies(final Collection<String> sources) {
		final Multimap<String, String> sourcesByModuleDependencies = groupSourcesByModuleDependencies(sources);
		
		final Multimap<String, String> modulesByModuleDependencies = 
				Multimaps.transformValues(sourcesByModuleDependencies, new Function<String,String>() { 
			@Override public String apply (final String source) 
			{return moduleNameOfSource(source).get();}});
		
		
		
		// Given we know module X requires modules Y,Z, etc.,
		// find an ordering of the modules such that X < Y iff Y requires X.
		// We can also assume that if D = {Y_0, Y_1, ..., Y_N} and X < D, Y < D,
		// then X < Y iff string(X) < string(Y)
		// X < X is false and of course, X = X.

		final ImmutableSortedSet<String> sortedSet = ImmutableSortedSet
				.copyOf(creatComparator(modulesByModuleDependencies),
						modulesByModuleDependencies.keySet());

		return sortedSet;

	}

	public Multimap<String, String> groupSourcesByModuleDependencies(
			final Collection<String> sources) {
		Preconditions.checkNotNull(sources);

		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap
				.builder();

		for (final String source : sources) {
			final File sourceFile = new File(source);

			Scanner scanner = null;
			try {
				scanner = new Scanner(sourceFile);

				while (scanner.hasNext()) {
					final String line = scanner.next();
					if (line == null)
						continue;
					if (line.startsWith("open") && !line.equals("open")) {
						final String moduleName = extractModuleName(line);

						if (isValidModuleName(moduleName)) {
							builder.put(moduleName, source);
						}
					}

					final Matcher matcher = PATTERN_MODULE_MIDDLE_OF_LINE
							.matcher(line);
					for (int i = 0; i < matcher.groupCount(); i++) {
						final String moduleName = extractModuleName(matcher
								.group(i));
						if (isValidModuleName(moduleName)) {
							builder.put(moduleName, source);
						}
					}
				}
			} catch (final FileNotFoundException e) {
				abstractMojo.getLog().error("problem with file: " + source, e);
				continue;
			} finally {
				if (scanner != null)
					scanner.close();
			}
		}
		return builder.build();
	}

	private String extractModuleName(final String line) {
		return line.substring("open".length(), line.length()).replace(";", "")
				.trim();
	}

	// TODO make much stricter
	private boolean isValidModuleName(final String moduleName) {
		return moduleName != null && !moduleName.trim().isEmpty();
	}

	private static final Comparator<String> creatComparator(
			final Multimap<String, String> dependencies) {
		
		return new Comparator<String>() {

			@Override
			public int compare(final String module1, final String module2) {
				if (Objects.equal(module1, module2))
					return 0;
				
				final Collection<String> module1Dependencies = dependencies.get(module1);
				final Collection<String> module2Dependencies = dependencies.get(module2);
			
				final boolean module1RequiresModule2 = module1Dependencies.contains(module2);
				final boolean module2RequiresModule1 = module2Dependencies.contains(module1);
				
				if (module2RequiresModule1 && module1RequiresModule2) {
					throw new IllegalStateException("circular dependency: " + module1 + " and " + module2);
				}
				
				// Pick one which requires the other.
				if (module2RequiresModule1)
					return -1;
				if (module1RequiresModule2)
					return 1;
				
				// Choose one with shortest number of dependencies
				final int cmp = Long.compare(module1Dependencies.size(), module2Dependencies.size());
				if (cmp != 0)
					return cmp;
				
				// When all else fails try default alphanumeric comparison
				return module1.compareTo(module2);
				
			}
		};
	}
}
