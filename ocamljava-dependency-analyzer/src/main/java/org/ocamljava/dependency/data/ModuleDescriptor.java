package org.ocamljava.dependency.data;

import java.io.File;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

class ModuleDescriptor {

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

	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final File moduleFile) {
		this(moduleName, moduleType, Optional.fromNullable(moduleFile));
	}

	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final Optional<File> moduleFile) {
		this.moduleName = Preconditions.checkNotNull(moduleName);
		this.moduleType = Preconditions.checkNotNull(moduleType);
		this.moduleFile = Preconditions.checkNotNull(moduleFile);
	}

	public String getModuleName() {
		return moduleName;
	}

	public Optional<File> getModuleFile() {
		return moduleFile;
	}

	public ModuleType getModuleType() {
		return moduleType;
	}

	final String moduleName;
	final ModuleType moduleType;
	final Optional<File> moduleFile;
	
	@Override
	public int hashCode() {
		return Objects.hashCode(moduleName, moduleType, moduleFile);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true; 
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ModuleDescriptor other = (ModuleDescriptor) obj;
		if (moduleFile == null) {
			if (other.moduleFile != null)
				return false;
		} else if (!moduleFile.equals(other.moduleFile))
			return false;
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
