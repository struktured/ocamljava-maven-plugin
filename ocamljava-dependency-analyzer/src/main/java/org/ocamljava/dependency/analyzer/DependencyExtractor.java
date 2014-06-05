package org.ocamljava.dependency.analyzer;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Comparator;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mandelbrot.ocamljava_maven_plugin.util.FileMappings;

import org.apache.maven.plugin.AbstractMojo;
import org.ocamljava.dependency.data.ModuleDescriptor;
import org.ocamljava.dependency.data.ModuleKey;
import org.ocamljava.dependency.data.ModuleKey.ModuleType;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

public class DependencyExtractor {

	static final Pattern MODULE_REGEX_START_OF_LINE = Pattern
			.compile("[\\w\\=\\s\\+\\-]*?([A-Z][a-zA-z0-9]+)\\.[\\w]*?");
	private final AbstractMojo abstractMojo;

	private final SortedSetMultimap<String, ModuleDescriptor> moduleToFilePath = TreeMultimap
			.create(new Comparator<String>() {
				@Override
				public int compare(final String paramT1, final String paramT2) {
					return paramT1.compareTo(paramT2);
				}
			}, new Comparator<ModuleDescriptor>() {

				@Override
				public int compare(final ModuleDescriptor s1, final ModuleDescriptor s2) {
					if (Objects.equal(s1,  s2))
						return 0;
					if (s1 == null)
						return 1;
					if (s2 == null)
						return -1;
					
					final ModuleType type1 = s1.getModuleType();
					final ModuleType type2 = s2.getModuleType();

					int cmp = ModuleType.dependencyCompareTo().compare(
							type1,
							type2);
					if (cmp != 0)
						return cmp;
					cmp = s1.getModuleName().compareTo(s2.getModuleName());
					
					if (cmp != 0)
						return cmp;

					final Optional<File> fromFile = s1.getModuleFile();
					final Optional<File> fromFile2 = s2.getModuleFile();
				
					
					if (fromFile.isPresent() && !fromFile2.isPresent())
						return -1;
					if (!fromFile.isPresent() && fromFile2.isPresent())
						return 1;

					return fromFile.get().compareTo(fromFile2.get());
					
			}});
	
	private final boolean scanningEnabled;

	public DependencyExtractor(final AbstractMojo abstractMojo) {
		this(abstractMojo, true);
	}
	
	public DependencyExtractor(final AbstractMojo abstractMojo, final boolean scanningEnabled) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
		this.scanningEnabled = scanningEnabled;
	}

	public Multimap<String, Optional<String>> groupSourcesByModuleDependencies(
			final Collection<String> sources, final File prefixToTruncate) {
		Preconditions.checkNotNull(sources);

		final ImmutableMultimap.Builder<String, Optional<String>> builder = ImmutableMultimap
				.builder();

		moduleToFilePath.clear();

		for (final String source : sources) {
			final File sourceFile = new File(source);

			// TODO move this util method to separate class
			final Optional<String> moduleNameOfSource = Analyzer
					.moduleNameOfSource(source);

			if (moduleNameOfSource.isPresent()) {
				moduleToFilePath.put(moduleNameOfSource.get(), new ModuleDescriptor.Builder()
						.setModuleFile(sourceFile)
						.setModuleKey(ModuleKey.fromFile(sourceFile))
						.setJavaPackageName(FileMappings.toPackage(prefixToTruncate, source))
						.build());

				// Hackish but convenient to add self to grouping so it appears
				// in the multimaps key set, but still
				// depend on nothing.
				builder.put(moduleNameOfSource.get(),
						Optional.<String> absent());
			
			
				if (scanningEnabled)
					scan(builder, source, sourceFile, moduleNameOfSource);
			}
		}
		return builder.build();
	}

	private void scan(
			final ImmutableMultimap.Builder<String, Optional<String>> builder,
			final String source, final File sourceFile,
			final Optional<String> moduleNameOfSource) {
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
						builder.put(moduleNameOfSource.get(),
								Optional.of(moduleName));
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
						builder.put(moduleNameOfSource.get(),
								Optional.of(moduleName));
					}
				}
			}
		} catch (final FileNotFoundException e) {
			abstractMojo.getLog().error("problem with file: " + source, e);
			return;
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}

	public SortedSetMultimap<String, ModuleDescriptor> getModuleToFilePath() {
		return Multimaps.unmodifiableSortedSetMultimap(moduleToFilePath);
	}

	private String extractModuleName(String line) {
		if (line.startsWith("open"))
			line = line.substring("open".length(), line.length());

		return line.replace(";", "").trim().toLowerCase();
	}

	// TODO make much stricter
	private boolean isValidModuleName(final String moduleName) {
		return moduleName != null && !moduleName.trim().isEmpty()
				&& !moduleName.contains(" ");
	}

	public Multimap<String, Optional<String>> groupSourcesByModuleDependencies(Collection<String> sources) {
		return groupSourcesByModuleDependencies(sources, Paths.get("").toFile());
	}

}
