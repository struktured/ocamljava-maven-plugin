package mandelbrot.ocamljava_maven_plugin.util;

import java.util.Arrays;
import java.util.jar.JarEntry;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class EntryInfo {

	private final byte[] bytes;
	private final JarEntry jarEntry;
	
	public JarEntry getJarEntry() {
		return jarEntry;
	}

	public EntryInfo(final byte[] bytes, final JarEntry jarEntry) {
		this.bytes = Preconditions.checkNotNull(bytes);
		this.jarEntry = Preconditions.checkNotNull(jarEntry);
	}

	public byte[] getBytes() {
		return bytes;
	}
	
	public static class Builder {
		private byte[] bytes;
		private JarEntry jarEntry;

		public Builder setBytes(final byte[] bytes) {
			this.bytes = bytes;
			return this;
		}
	
		public EntryInfo build() {
			return new EntryInfo(bytes, jarEntry);
		}

		public final Builder setEntry(final JarEntry jarEntry) {
			this.jarEntry = jarEntry;
			return this;
		}
	}
	
	public static Builder builder() {
		return new Builder();
	}
	
	public String toString() {
		return Objects.toStringHelper(this).toString();
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(bytes, jarEntry);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final EntryInfo other = (EntryInfo) obj;
		if (!Arrays.equals(bytes, other.bytes))
			return false;
		if (jarEntry == null) {
			if (other.jarEntry != null)
				return false;
		} else if (!jarEntry.equals(other.jarEntry))
			return false;
		return true;
	}
	
	
}
