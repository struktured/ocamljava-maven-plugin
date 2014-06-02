package org.ocamljava.dependency.data;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

public class ModuleKey {

	public enum ModuleType {
		
		// TODO factor out constants from OcamlJavaConstants class into the ocamljava  mavencommon module
		IMPL("ml"), INTERFACE("mli");
		
		private final String extension;

		private ModuleType(final String extension) {
			this.extension = Preconditions.checkNotNull(extension);
		}
		
		public String getExtension() {
			return extension;
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
