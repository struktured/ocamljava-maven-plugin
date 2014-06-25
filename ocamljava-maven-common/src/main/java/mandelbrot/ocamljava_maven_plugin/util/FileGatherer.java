package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.logging.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;

public class FileGatherer {

	private final AbstractMojo mojo;

	public FileGatherer(final AbstractMojo abstractMojo) {
		this.mojo = Preconditions.checkNotNull(abstractMojo);
	}
	
	public Multimap<String, String> gatherFiles(final File root, final Set<String> extensions) {
		final ImmutableMultimap.Builder<String, String> files = ImmutableMultimap
				.builder();
		if (root.isFile() && isAllowedExtension(root, extensions)) {
			files.put(org.codehaus.plexus.util.FileUtils.getExtension(root
					.getName()), root.getPath());
			return files.build();
		}

		if (!root.isDirectory() || root.listFiles() == null)
			return files.build();

		for (final File file : root.listFiles()) {
			if (file.isDirectory()) {
				getLog().info("scanning directory: " + file);

				files.putAll(gatherFiles(file, extensions));
			} else {
				if (isAllowedExtension(file, extensions)) {
					getLog().info("adding ocaml source file: " + file);
					files.put(org.codehaus.plexus.util.FileUtils
							.getExtension(file.getName()), file.getPath());
				}
			}
		}
		return files.build();
	}

	private Log getLog() {
		return mojo.getLog();
	}

	private boolean isAllowedExtension(final File file, final Set<String> extensions) {
		final String extension = org.codehaus.plexus.util.FileUtils.getExtension(file.getPath());
		if (extension == null)
			return false;
		return extensions.contains(extension.toLowerCase());
	}

}
