package mandelbrot.ocamljava_maven_plugin.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.apache.maven.plugin.AbstractMojo;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;

public class JarAppender {

	public static int BUFFER_SIZE = 10240;

	private final AbstractMojo abstractMojo;

	public JarAppender(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
	}

	public void addFiles(final String archiveFile,
			final Collection<String> toBeJaredFiles) {

		final Collection<File> toBeJaredArray = Collections2.transform(
				toBeJaredFiles,

				new Function<String, File>() {
					@Override
					public File apply(final String path) {
						return new File(path);
					}
				});

		try {
			final byte buffer[] = new byte[BUFFER_SIZE];
			
			final JarEntryReader jarEntryReader = new JarEntryReader(abstractMojo);
			final Collection<EntryInfo> entryInfos = jarEntryReader.readEntries(archiveFile, buffer);
			
			// Open archive file
			final FileOutputStream stream = new FileOutputStream(archiveFile);
			final JarOutputStream out = jarEntryReader.getManifest().isPresent() ? 
				new JarOutputStream(stream, jarEntryReader.getManifest().get()) : new JarOutputStream(stream);

				
			for (final EntryInfo entryInfo : entryInfos) {
				out.putNextEntry(entryInfo.getJarEntry());	
				abstractMojo.getLog().info("Adding original entry: " + entryInfo.getJarEntry().getName());
				pipe(out, buffer, new ByteArrayInputStream(entryInfo.getBytes()));
			}

			for (final File file : toBeJaredArray) {

				if (file == null || !file.exists() || file.isDirectory())
					continue; // Just in case...
				abstractMojo.getLog().info("Adding new entry: " + file.getName());
				addEntry(file, out, buffer);
			}
			
			out.close();
			stream.close();
			abstractMojo.getLog().info("Adding completed OK");
		} catch (final Exception ex) {
			abstractMojo.getLog().error("Error: " + ex.getMessage(), ex);
		}

	}

	private void addEntry(final File file, final JarOutputStream out,
			final byte[] buffer) throws IOException {
		final JarEntry jarAdd = new JarEntry(file.getPath());
		jarAdd.setTime(file.lastModified());
		out.putNextEntry(jarAdd);

		// Write file to archive
		final FileInputStream in = new FileInputStream(file);
		pipe(out, buffer, in);
		in.close();

	}

	private void pipe(final OutputStream out, final byte[] buffer,
			final InputStream in) throws IOException {
		while (true) {
			final int nRead = in.read(buffer, 0, buffer.length);
			if (nRead <= 0)
				break;
			out.write(buffer, 0, nRead);
		}
	}
}