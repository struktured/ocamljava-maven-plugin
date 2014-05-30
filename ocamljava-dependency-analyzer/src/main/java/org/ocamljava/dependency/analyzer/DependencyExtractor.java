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

	
 static final Pattern MODULE_REGEX_START_OF_LINE = 
	Pattern.compile("[\\w\\=\\s\\+\\-]*?([A-Z][a-zA-z0-9]+)\\.[\\w]*?");
	private final AbstractMojo abstractMojo;

	public DependencyExtractor(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
	}
	
	public Multimap<String, Optional<String>> groupSourcesByModuleDependencies(
			final Collection<String> sources) {
		Preconditions.checkNotNull(sources);

		final ImmutableMultimap.Builder<String, Optional<String>> builder = ImmutableMultimap
				.builder();

		for (final String source : sources) {
			final File sourceFile = new File(source);

			// TODO move this util method to separate class
			final Optional<String> moduleNameOfSource = Analyzer.moduleNameOfSource(source);


			// Hackish but convenient to  add self to grouping so it appears in the multimaps key set, but still
			// depend on nothing.
			if (moduleNameOfSource.isPresent()) {
				builder.put(moduleNameOfSource.get(), Optional.<String>absent());
			}
			
			Scanner scanner = null;
			try {
				scanner = new Scanner(sourceFile);

				while (scanner.hasNext()) {
					final String line = scanner.nextLine();
					if (line == null)
						continue;
					if (line.startsWith("open") && !line.equals("open")) {
						final String moduleName = extractModuleName(line);

						if (isValidModuleName(moduleName)) {
							builder.put(moduleNameOfSource.get(), Optional.of(moduleName));
						}
					}

					final Matcher matcher = MODULE_REGEX_START_OF_LINE
							.matcher(line);
					if (!matcher.matches())
						continue;
					
					for (int i = 1; i <= matcher.groupCount(); i++) {
						final String moduleName = extractModuleName(matcher
								.group(i));
						if (isValidModuleName(moduleName)) {
							builder.put(moduleNameOfSource.get(), Optional.of(moduleName));
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

	private String extractModuleName(String line) {
		if (line.startsWith("open"))
			line = line.substring("open".length(), line.length());
		
		return line.replace(";", "").trim().toLowerCase();
	}

	// TODO make much stricter
	private boolean isValidModuleName(final String moduleName) {
		return moduleName != null && !moduleName.trim().isEmpty() && !moduleName.contains(" ");
	}

}