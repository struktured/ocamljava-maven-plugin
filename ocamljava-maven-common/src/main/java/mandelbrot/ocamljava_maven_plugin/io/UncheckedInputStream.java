package mandelbrot.ocamljava_maven_plugin.io;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Preconditions;


/***
 * Wraps an output stream and simply suppresses checked {@link IOException}s from the method signatures.
 * @author carm
 *
 */
public class UncheckedInputStream<T extends InputStream> extends InputStream {

	final T inputStream;
		
	private UncheckedInputStream(final T streamToWrap) {
		this.inputStream = Preconditions.checkNotNull(streamToWrap);
	}

	@SuppressWarnings("unchecked")
	private UncheckedInputStream(final File file) {
		try {
			this.inputStream = (T) new FileInputStream(file);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void close() {
		try {
			inputStream.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	

	public T getWrappedInputStream() {
		return inputStream;
	}
	
	public static UncheckedInputStream<ByteArrayInputStream> fromBytes(final byte[] bytes) {
		return new UncheckedInputStream<ByteArrayInputStream>(new ByteArrayInputStream(bytes));
	}

	public static UncheckedInputStream<FileInputStream> fromFile(final File file) {
		return new UncheckedInputStream<FileInputStream>(file);
	}

	public static <T extends InputStream> InputStream wrap(final T InputStream) {
		return new UncheckedInputStream<T>(InputStream);
	}

	@Override
	public int read() {
		try { return inputStream.read(); } catch (final IOException e) { throw new RuntimeException(e); }
	}
	
	@Override
	public int read(final byte[] b) {
		try { return inputStream.read(b); } catch (final IOException e) { throw new RuntimeException(e); }
	}
	
	@Override
	public int read(byte[] b, int off, int len) {
		try { return inputStream.read(b, off, len); } catch (final IOException e) { throw new RuntimeException(e); }
	}
	
}

