package org.ocamljava.dependency.data;

import java.util.Comparator;

import org.codehaus.plexus.util.FileUtils;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class ModuleKey {

	public static class Builder {
		private String moduleName;
		private ModuleType moduleType;
		
		public Builder setModuleName(final String moduleName) {
			this.moduleName = moduleName; return this;
		}
		
		public Builder setModuleType(final ModuleType moduleType) {
			this.moduleType = moduleType; return this;
		}
		
		public ModuleKey build() {
			return new ModuleKey(moduleName, moduleType);
		}
		
	}

	public enum ModuleType {
		
		IMPL("ml"), INTERFACE("mli");
		
		private final String extension;

		private ModuleType(final String extension) {
			this.extension = Preconditions.checkNotNull(extension);
		}
		
		public String getExtension() {
			return extension;
		}
		
		public static Comparator<ModuleType> dependencyCompareTo() {
			return new Comparator<ModuleType>() {
				@Override
				public int compare(final ModuleType o1, final ModuleType o2) {
					
					if (Objects.equal(o1, o2))
						return 0;
					
					return INTERFACE.equals(o1) ? -1 : 1;
					
				}
			};
		}

		public static Optional<ModuleType> fromFile(final String source) {
			final String extension = FileUtils.getExtension(source);
			
			for (final ModuleType moduleType :values()) {
				if (moduleType.getExtension().equals(extension)) {
					return Optional.of(moduleType);
				}
			}
			return Optional.absent();
		}
	}

	public ModuleKey(final String moduleName,
			final ModuleType moduleType) {
		this.moduleName = Preconditions.checkNotNull(moduleName);
		this.moduleType = Preconditions.checkNotNull(moduleType);
	}

	public String getModuleName() {
		return moduleName;
	}

	public ModuleType getModuleType() {
		return moduleType;
	}

	final String moduleName;
	final ModuleType moduleType;
	
	@Override
	public int hashCode() {
		return Objects.hashCode(moduleName, moduleType);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true; 
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final ModuleDescriptor other = (ModuleDescriptor) obj;
		if (moduleName == null) {
			if (other.moduleName != null)
				return false;
		} else if (!moduleName.equals(other.moduleName))
			return false;
		if (moduleType != other.moduleType)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).toString();
	}

	
}
