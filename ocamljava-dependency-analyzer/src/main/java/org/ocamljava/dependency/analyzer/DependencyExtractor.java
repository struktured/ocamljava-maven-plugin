package org.ocamljava.dependency.analyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.AbstractMojo;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class DependencyExtractor {

	private static final String MODULE_REGEX_START_OF_LINE = "[A-Z][a-zA-z0-9_]+\\.";
	private static final String MODULE_REGEX_MIDDLE_OF_LINE = "[\\s\\,\\;\\=\\+]"
			+ MODULE_REGEX_START_OF_LINE;

	final Pattern PATTERN_MODULE_MIDDLE_OF_LINE = Pattern
			.compile(MODULE_REGEX_MIDDLE_OF_LINE);
	final Pattern PATTERN_MODULE_START_OF_LINE = Pattern
			.compile(MODULE_REGEX_START_OF_LINE);


	private final AbstractMojo abstractMojo;

	public DependencyExtractor(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
	}
	
	public Multimap<String, String> groupSourcesByModuleDependencies(
			final Collection<String> sources) {
		Preconditions.checkNotNull(sources);

		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap
				.builder();

		for (final String source : sources) {
			final File sourceFile = new File(source);

			// TODO move this util method to separate class
			final Optional<String> moduleNameOfSource = Analyzer.moduleNameOfSource(source);

			// add self to grouping so it appears in the multimaps key set, but
			// depend on nothing.
			if (moduleNameOfSource.isPresent()) {
				builder.put(moduleNameOfSource.get(), source);
			}

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

}
