package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Set;

import org.apache.maven.plugin.AbstractMojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class JarExtractor {

	public static int BUFFER_SIZE = 10240;

	private final AbstractMojo abstractMojo;

	public JarExtractor(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
	}
	
	public Collection<String> extractFiles(final String archiveFile, final String targetPath) {
		return extractFiles(archiveFile, targetPath, null);
	}
	
	public Collection<String> extractFiles(final String archiveFile, final String targetPath, final Set<String> allowedExtensions) {

		final ImmutableList.Builder<String> filesBuilder = ImmutableList.builder();

		try {

			final Collection<EntryInfo> entryInfos = new JarEntryReader(abstractMojo).readEntries(archiveFile, allowedExtensions);
			
			final FileOutputStream stream = new FileOutputStream(archiveFile);

			for (final EntryInfo entryInfo : entryInfos) {
				final File moduleFile = new File(targetPath, entryInfo
						.getJarEntry().getName());
				
				if (!moduleFile.exists()) {
					new File(moduleFile.getParent()).mkdirs();
					moduleFile.createNewFile();
				}
				
				filesBuilder.add(moduleFile.getPath());
				final FileOutputStream outputStream = new FileOutputStream(
						moduleFile);
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