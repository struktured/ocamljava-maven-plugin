package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;
import org.codehaus.plexus.util.FileUtils;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class FilesByExtensionGatherer {

	private final AbstractMojo project;
	private final Set<String> extensions;
	
	public FilesByExtensionGatherer(final AbstractMojo project, final String extension) {
		this(project, ImmutableSet.of(extension));
	}
	
	public FilesByExtensionGatherer(final AbstractMojo project, final Set<String> extensions) {
		this.project = Preconditions.checkNotNull(project);
		this.extensions = Preconditions.checkNotNull(extensions);
	}

	public Multimap<String, String> gather(final File root) {
		final ImmutableMultimap.Builder<String,String> files = ImmutableMultimap.builder();
		
		if (root == null)
			return files.build();
		
		final Optional<String> rootExtension;
		if ((rootExtension = extensionOf(root)).isPresent()) {
			files.put(rootExtension.get(), root.getPath());
			return files.build();
		}

		if (!root.isDirectory() || root.listFiles() == null)
			return files.build();

		for (final File file : root.listFiles()) {
			if (file.isDirectory()) {
				project.getLog().info("scanning directory: " + file);

				files.putAll(gather(file));
			} else {
				final Optional<String> extension;
				if ((extension = extensionOf(file)).isPresent()) {
					project.getLog().info("adding file : " + file);
					files.put(extension.get(), file.getPath());
				}
			}
		}
		return files.build();
	}

	private Optional<String> extensionOf(final File root) {
		if (root == null || !root.isFile())
			return Optional.absent();
		
		final String extension = FileUtils.getExtension(root.getPath());
		if (extensions.contains(extension))
			return Optional.of(extension);
		return Optional.absent();
	}
}
 