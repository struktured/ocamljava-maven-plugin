package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class JarExtractor {

	public static int BUFFER_SIZE = 10240;

	private final AbstractMojo abstractMojo;

	public JarExtractor(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
	}

	public Collection<String> addFiles(final String archiveFile, final String targetPath) {

		final ImmutableList.Builder<String> filesBuilder = ImmutableList.builder();

		try {

			final Collection<EntryInfo> entryInfos = new JarEntryReader(abstractMojo).readEntries(archiveFile);
			
		// Open archive file
			final FileOutputStream stream = new FileOutputStream(archiveFile);

			for (final EntryInfo entryInfo : entryInfos) {
				final File cmiFile = new File(targetPath, entryInfo
						.getJarEntry().getName());
				
				if (!cmiFile.exists()) {
					new File(cmiFile.getParent()).mkdirs();
					cmiFile.createNewFile();
				}
				
				filesBuilder.add(cmiFile.getPath());
				final FileOutputStream outputStream = new FileOutputStream(
						cmiFile);
				outputStream.write(entryInfo.getBytes());
				try {
					outputStream.close();
				} catch (final Exception e) {
					abstractMojo.getLog().warn(
							"error processing entry: " + entryInfo, e);
				}
			}
			stream.close();
		} catch (final Exception ex) {
			abstractMojo.getLog().error("Error: " + ex.getMessage(), ex);
		}
		return filesBuilder.build();

	}
}