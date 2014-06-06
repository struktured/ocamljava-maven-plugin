package org.ocamljava.dependency.data;

import java.io.File;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;

public class ModuleDescriptor extends ModuleKey implements Comparable<ModuleDescriptor> {

	private final String javaPackageName;

	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final File moduleFile, final String javaPackageName) {
		this(moduleName, moduleType, Optional.fromNullable(moduleFile), javaPackageName);
	}

	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final Optional<File> moduleFile, final String javaPackageName) {
		super(moduleName, moduleType);
		this.moduleFile = Preconditions.checkNotNull(moduleFile);
		this.javaPackageName = Preconditions.checkNotNull(javaPackageName);
	}


	public String getJavaPackageName() {
		return javaPackageName;
	}
	
	public Optional<File> getModuleFile() {
		return moduleFile;
	}

	final Optional<File> moduleFile;
	
	@Override
	public int hashCode() {
		return Objects.hashCode(moduleName, moduleType, moduleFile, javaPackageName);
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
		return Objects.equal(javaPackageName, other.javaPackageName);
	}
	
	
	public static class Builder extends ModuleKey.Builder {
		private File moduleFile;
		private String javaPackageName;
		
		@Override
		public Builder setModuleName(final String moduleName) {
			super.setModuleName(moduleName);
			return this;
		}
		
		@Override
		public Builder setModuleType(final ModuleType moduleType) {
			super.setModuleType(moduleType);
			return this;
		}
		
		public Builder setModuleFile(final File moduleFile) {
			this.moduleFile = moduleFile;
			return this;
		}
		
		public Builder setJavaPackageName(final String javaPackageName) {
			this.javaPackageName = javaPackageName;
			return this;
		}
		
		@Override
		public ModuleDescriptor build() {
			final ModuleKey key = super.build();
			return new ModuleDescriptor(key.getModuleName(), key.getModuleType(), moduleFile, javaPackageName);
		}

		public Builder setModuleKey(final ModuleKey key) {
			setModuleName(key.getModuleName());
			setModuleType(key.getModuleType());
			
			return this;
		}
		
	}
	
	public static Function<ModuleDescriptor,String> toFileTransform() {
		return new Function<ModuleDescriptor, String>() {
			public String apply(final ModuleDescriptor desc) {
					return desc.getModuleFile().get().getPath();
			}
		};
	}

	public boolean nameEquals(final String module) {
		return Objects.equal(getModuleName(), module);
	}

	@Override
	public int compareTo(final ModuleDescriptor arg0) {
		if (arg0 == null)
			return -1;
		
		int compareTo = Optional.fromNullable(getModuleName()).or("").compareTo(Optional.fromNullable(arg0.getModuleName()).or(""));
		
		if (compareTo != 0)
			return compareTo;
		
		compareTo = getModuleFile().or(new File("")).compareTo(arg0.getModuleFile().or(new File("")));
		
		if (compareTo != 0)
			return compareTo;
		
		compareTo = Optional.fromNullable(getModuleType()).or(ModuleType.IMPL)
				.compareTo(Optional.fromNullable(arg0.getModuleType()).or(ModuleType.IMPL));
		
		if (compareTo != 0)
			return compareTo;
		
		compareTo = Optional.fromNullable(getJavaPackageName()).or("")
				.compareTo(Optional.fromNullable(arg0.getJavaPackageName()).or(""));
		
		return compareTo;
		
	}

}
