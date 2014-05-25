package mandelbrot.ocamljava_maven_plugin.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.maven.plugin.AbstractMojo;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class JarEntryReader {


	private volatile Optional<Manifest> manifest;
	
	public Optional<Manifest> getManifest() {
		return manifest == null ? Optional.<Manifest>absent() : manifest;
	}

	@SuppressWarnings("unused")
	private final AbstractMojo abstractMojo;

	public JarEntryReader(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
	}
	private static final int BUFFER_SIZE = 16384;
	
	public Collection<EntryInfo> readEntries(final String archiveFile) throws IOException {
		return readEntries(archiveFile, null);
	}
	
	public Collection<EntryInfo >readEntries(final String archiveFile, final byte[] optionalBuffer) throws IOException {

	final byte[] buffer = optionalBuffer == null ? new byte[BUFFER_SIZE] : optionalBuffer;

	final FileInputStream in = new FileInputStream(archiveFile);
	final BufferedInputStream bufferedInputStream = new BufferedInputStream(in);
	final JarInputStream jarInputStream = new JarInputStream(bufferedInputStream);
	this.manifest = Optional.fromNullable(jarInputStream.getManifest());
	
	final ImmutableList.Builder<EntryInfo> builder = ImmutableList.builder();
	
	JarEntry nextEntry = null;
	
	while ((nextEntry = jarInputStream.getNextJarEntry()) != null) {
		builder.add(getEntryInfo(nextEntry, jarInputStream, buffer));
	}
	
	return builder.build();
	}
		
	private EntryInfo getEntryInfo(final JarEntry nextEntry, final JarInputStream in,
		final byte[] buffer) throws IOException {
		
		final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		
		pipe(byteArrayOutputStream, buffer, in);
		
		return EntryInfo.builder().setBytes(byteArrayOutputStream.toByteArray()).setEntry(nextEntry).build();

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
