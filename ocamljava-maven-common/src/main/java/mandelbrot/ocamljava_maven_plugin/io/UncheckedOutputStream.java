package mandelbrot.ocamljava_maven_plugin.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.google.common.base.Preconditions;


/***
 * Wraps an output stream and simply suppresses checked {@link IOException}s from the method signatures.
 * @author carm
 *
 */
public class UncheckedOutputStream<T extends OutputStream> extends OutputStream {

	final T outputStream;
		
	private UncheckedOutputStream(final T streamToWrap) {
		this.outputStream = Preconditions.checkNotNull(streamToWrap);
	}

	@SuppressWarnings("unchecked")
	private UncheckedOutputStream(final File file) {
		try {
			this.outputStream = (T) new FileOutputStream(file);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(int arg0) throws IOException {
		outputStream.write(arg0);
	}

	@Override
	public void close() {
		try {
			outputStream.close();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void flush() {
		try {
			outputStream.flush();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void write(final byte[] b) {
		try {
			outputStream.write(b);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public void write(final byte[] b, final int off, final int len) {
		try {
			outputStream.write(b, off, len);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public T getWrappedOutputStream() {
		return outputStream;
	}
	
	public static UncheckedOutputStream<ByteArrayOutputStream> fromBytes(final int size) {
		return new UncheckedOutputStream<ByteArrayOutputStream>(new ByteArrayOutputStream(size));
	}

	public static UncheckedOutputStream<FileOutputStream> fromFile(final File file) {
		return new UncheckedOutputStream<FileOutputStream>(file);
	}

	public static <T extends OutputStream> OutputStream wrap(final T outputStream) {
		return new UncheckedOutputStream<T>(outputStream);
	}
}
