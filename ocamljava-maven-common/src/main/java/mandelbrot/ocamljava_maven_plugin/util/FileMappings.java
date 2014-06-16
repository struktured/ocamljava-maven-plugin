package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.util.Collection;

import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class FileMappings {

	private static final char PACKAGE_NAME_SEPARATOR = '.';

	private FileMappings() {
	}

	public static Multimap<String, String> buildPackageMap(
			final File prefixToTruncate, final Collection<String> artifactFiles) {

		final ImmutableMultimap.Builder<String, String> builder = ImmutableMultimap
				.builder();

		for (final String artifactFilePath : artifactFiles) {
			final String packageName = toPackage(prefixToTruncate,
					new File(artifactFilePath).getParent());
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

	public static Function<String, String> toPackageTransform(
			final File prefixToTruncate) {

		return new Function<String, String>() {
			@Override
			public String apply(String path) {
				if (path == null)
					return "";
				
				path = StringUtils.isBlank(FileUtils.getExtension(path)) ? path : new File(path).getParent();
		
				if (path == null)
					return "";
				
				if (prefixToTruncate != null && prefixToTruncate.getPath() != null && prefixToTruncate.exists()) {

					final int length = prefixToTruncate.getPath().length();
					if (path.length() >= length)
						path = path.substring(length);
				}

				path = path.replace(File.separatorChar, PACKAGE_NAME_SEPARATOR);

				path = StringTransforms.trim(path, PACKAGE_NAME_SEPARATOR);
				
				return path;

			}
		};
	}

	public static Collection<String> toPackage(final File prefixToTruncate,
			final Collection<String> paths) {
		return Collections2.transform(paths,
				toPackageTransform(prefixToTruncate));
	}

	public static String toPackage(final File prefixToTruncate,
			final String path) {
		return toPackage(prefixToTruncate, ImmutableSet.<String> of(path))
				.iterator().next();
	}
	
	public static String toPackagePath(final File prefixToTruncate,
			final String path) {
		return toPackage(prefixToTruncate, ImmutableSet.<String> of(path))
				.iterator().next().replace(PACKAGE_NAME_SEPARATOR, File.separatorChar);
	}

	public static String toPackagePath(final String prefixToTruncate,
			final String path) {
		return toPackagePath(prefixToTruncate == null ? (File) null : new File(prefixToTruncate), path);
	}
}
