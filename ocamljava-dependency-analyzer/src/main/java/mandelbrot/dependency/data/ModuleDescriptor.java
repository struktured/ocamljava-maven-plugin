package mandelbrot.dependency.data;

import java.io.File;
import java.util.Comparator;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
/***
 * A description of a single ocaml module. Serializable via JSON format.
 * @author carm
 *
 */
@JsonTypeName("moduleDescriptor")
public class ModuleDescriptor extends ModuleKey implements
		Comparable<ModuleDescriptor> {

	private static final String MODULE_FILE_PROPERTY = "moduleFile";
	private static final String MODULE_NAME_PROPERTY = "moduleName";
	private static final String JAVA_PACKAGE_NAME_PROPERTY = "javaPackageName";
	private static final String MODULE_TYPE_PROPERTY = "moduleType";
	
	private final String javaPackageName;

	@JsonIgnore
	private final Optional<File> moduleFile;

	@JsonProperty(MODULE_FILE_PROPERTY)
	public String getModuleFileName() {
		if (moduleFile.isPresent())
			return moduleFile.get().getPath();
		else
			return null;
	}

	@JsonCreator
	public ModuleDescriptor(final @JsonProperty(MODULE_NAME_PROPERTY) String moduleName,
			final @JsonProperty(MODULE_TYPE_PROPERTY) ModuleType moduleType, 
			final @JsonProperty(MODULE_FILE_PROPERTY) String moduleFile,
			final @JsonProperty(JAVA_PACKAGE_NAME_PROPERTY) String javaPackageName) {
		this(moduleName, moduleType, StringUtils.isBlank(moduleFile)  ? null : new File(moduleFile), javaPackageName);
	}
	
	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final File moduleFile,
			final String javaPackageName) {
		this(moduleName, moduleType, Optional.fromNullable(moduleFile),
				javaPackageName);
	}

	public ModuleDescriptor(final String moduleName,
			final ModuleType moduleType, final Optional<File> moduleFile,
			final String javaPackageName) {
		super(moduleName, moduleType);
		this.moduleFile = Preconditions.checkNotNull(moduleFile);
		this.javaPackageName = Preconditions.checkNotNull(javaPackageName);
	}

	public String getJavaPackageName() {
		return javaPackageName;
	}

	@JsonIgnore
	public Optional<File> getModuleFile() {
		return moduleFile;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(moduleName, moduleType, moduleFile,
				javaPackageName);
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
		
		if (!Objects.equal(moduleFile, other.moduleFile))
			return false;
		if (!Objects.equal(moduleName, other.moduleName))
			return false;
		if (!Objects.equal(moduleType, other.moduleType))
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
			
			if (isModuleKeySet())
				return this;
			else
				return setModuleKey(ModuleKey.fromFile(moduleFile));
		}

		public Builder setJavaPackageName(final String javaPackageName) {
			this.javaPackageName = javaPackageName;
			return this;
		}

		@Override
		public ModuleDescriptor build() {
			final ModuleKey key = super.build();
			return new ModuleDescriptor(key.getModuleName(),
					key.getModuleType(), moduleFile, javaPackageName);
		}

		public Builder setModuleKey(final ModuleKey key) {
			setModuleName(key.getModuleName());
			setModuleType(key.getModuleType());

			return this;
		}

		public Builder copyOf(final ModuleDescriptor paramF) {
			setModuleKey(paramF);
			setModuleFile(paramF.getModuleFile().orNull());
			setModuleName(paramF.getModuleName());
			setJavaPackageName(paramF.getJavaPackageName());
			return this;
		}

	}

	public static Function<ModuleDescriptor, String> toFileTransform() {
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
	public int compareTo(final ModuleDescriptor other) {
		if (other == null)
			return -1;

		int compareTo;

		compareTo = ModuleType.dependencyCompareTo().compare(getModuleType(),
				other.getModuleType());

		if (compareTo != 0)
			return compareTo;

		compareTo = Optional.fromNullable(getModuleName()).or("")
				.compareTo(Optional.fromNullable(other.getModuleName()).or(""));

		if (compareTo != 0)
			return compareTo;

		compareTo = getModuleFile().or(new File("")).compareTo(
				other.getModuleFile().or(new File("")));

		if (compareTo != 0)
			return compareTo;

		compareTo = Optional
				.fromNullable(getJavaPackageName())
				.or("")
				.compareTo(
						Optional.fromNullable(other.getJavaPackageName()).or(""));

		return compareTo;

	}

	public static Comparator<? super ModuleDescriptor> comparator() {
		return new Comparator<ModuleDescriptor>() {
			@Override
			public int compare(final ModuleDescriptor paramT1,
					final ModuleDescriptor paramT2) {
				
				if (Objects.equal(paramT1, paramT2))
					return 0;
				
				if (paramT1 == null)
					return 1;
				
				return paramT1.compareTo(paramT2);
			}
		};
	}

	public static Builder builder() {
		return new Builder();
	}

}
