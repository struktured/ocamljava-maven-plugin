package mandelbrot.dependency.analyzer;

import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import mandelbrot.dependency.data.ModuleDescriptor;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultimap.Builder;
import com.google.common.collect.SortedSetMultimap;

// TODO Unused..found another algorithm which just iterates in order and inserts into an immultible multimap builder
// as it sees each package name / module descriptor entry.
public class PackageComparator implements Comparator<String> {

	private final ImmutableMultimap<String, ModuleDescriptor> modulesByPackageName;

	public PackageComparator(final SortedSetMultimap<String, ModuleDescriptor> resolvedModuleDependencies) {
		this.resolvedModuleDependencies = Preconditions.checkNotNull(resolvedModuleDependencies);
		
		final Set<Entry<String,ModuleDescriptor>> entries = resolvedModuleDependencies.entries();
		
		final Builder<String, ModuleDescriptor> builder = ImmutableMultimap.builder();
		for (final Entry<String, ModuleDescriptor> entry : entries) {
			final String javaPackageName = entry.getValue().getJavaPackageName();
			
			builder.put(javaPackageName, entry.getValue());
			
		}
		
		this.modulesByPackageName = builder.build();
		
		
	}

	public ImmutableMultimap<String, ModuleDescriptor> getModulesByPackageName() {
		return modulesByPackageName;
	}

	final SortedSetMultimap<String, ModuleDescriptor> resolvedModuleDependencies;

	@Override
	public int compare(final String arg0, final String arg1) {
		if (Objects.equal(arg0,  arg1))
			return 0;
	
		final ImmutableCollection<ModuleDescriptor> immutableCollection = modulesByPackageName.get(arg0);
		final ImmutableCollection<ModuleDescriptor> immutableCollection2 = modulesByPackageName.get(arg1);
		
		for (final ModuleDescriptor moduleDescriptor : immutableCollection) {
			for (final ModuleDescriptor moduleDescriptor2 : immutableCollection2) {
				
				final int cmp = dependencyCompare(moduleDescriptor, moduleDescriptor2); 
				
				if (cmp != 0)
					return cmp;
				}
			}
		
		return nullSafeStringCompare(arg0, arg1);
		
	}

	private int nullSafeStringCompare(final String arg0, final String arg1) {
		return Optional.fromNullable(arg0).or("").compareTo(Optional.fromNullable(arg1).or(""));
	}

	private int dependencyCompare(final ModuleDescriptor moduleDescriptor,
			final ModuleDescriptor moduleDescriptor2) {
		
		if (Objects.equal(moduleDescriptor, moduleDescriptor2))
			return 0;

		final List<String> orderedModules = ImmutableList.copyOf(resolvedModuleDependencies.keySet());
		
		for (final String module : orderedModules) {
			if (moduleDescriptor.nameEquals(module)) {
				return -1;
			}
			
			if (moduleDescriptor2.nameEquals(module)) {
				return 1;
			}
		}
		
		return nullSafeStringCompare(moduleDescriptor.getModuleName(), moduleDescriptor2.getModuleName());
		
	}

	
	
}
