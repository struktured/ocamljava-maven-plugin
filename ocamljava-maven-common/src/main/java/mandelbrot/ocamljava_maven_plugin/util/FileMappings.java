package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.util.Collection;

import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class FileMappings {

	private static final char PACKAGE_NAME_SEPARATOR = '.';

	private FileMappings() {}
	
	public static Multimap<String, String> buildPackageMap(final File prefixToTruncate,
			final Collection<String> artifactFiles) {

		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap
				.builder();

		for (final String artifactFilePath : artifactFiles) {
			final String packageName = toPackage(prefixToTruncate, artifactFilePath);
			builder.put(packageName, artifactFilePath);
		}
		return builder.build();
	}

	public static Multimap<String, String> buildPathMap(
			final Collection<String> artifactFiles) {

		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap
				.builder();

		for (final String artifactFilePath : artifactFiles) {
			final String parent = new File(artifactFilePath).getParent();
			builder.put(parent, artifactFilePath);
		}
		return builder.build();
	}

	public static Function<String, String> toPackageTransform(final File prefixToTruncate) {

		return new Function<String, String>() {
			@Override
			public String apply(String path) {
				if (path == null)
					return null;
				
				if (prefixToTruncate != null) {
					path = path.replaceFirst(prefixToTruncate.getPath(), "");
				}
				
				String result = StringUtils.isBlank(path) ? path : path.replace(
						File.separatorChar, PACKAGE_NAME_SEPARATOR);
				
				while (!result.isEmpty() && result.startsWith(String.valueOf(PACKAGE_NAME_SEPARATOR)))
					result = result.substring(1, result.length());
				
				while (!result.isEmpty() && result.endsWith(String.valueOf(PACKAGE_NAME_SEPARATOR)))
					result = result.substring(0, result.length()-1);
				return result;
					
			}
		};
	}


	public static Collection<String> toPackage(final File prefixToTruncate, final Collection<String> paths) {
		return Collections2.transform(paths, toPackageTransform(prefixToTruncate));
	}

	public static String toPackage(final File prefixToTruncate, final String path) {
		return toPackage(prefixToTruncate, ImmutableSet.<String>of(path)).iterator().next();
	}
}

