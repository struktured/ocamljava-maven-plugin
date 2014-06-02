package org.ocamljava.dependency.data;

import java.io.File;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

class ModuleDescriptor extends ModuleKey {

	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final File moduleFile) {
		this(moduleName, moduleType, Optional.fromNullable(moduleFile));
	}

	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final Optional<File> moduleFile) {
		super(moduleName, moduleType);
		this.moduleFile = Preconditions.checkNotNull(moduleFile);
	}


	public Optional<File> getModuleFile() {
		return moduleFile;
	}

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
		final ModuleDescriptor other = (ModuleDescriptor) obj;
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
}
