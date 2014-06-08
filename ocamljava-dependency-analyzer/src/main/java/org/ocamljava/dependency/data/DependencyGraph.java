package org.ocamljava.dependency.data;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import mandelbrot.ocamljava_maven_plugin.util.StringTransforms;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.ObjectMapper;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.SortedSetMultimap;


@JsonTypeName("dependencyGraph")
public class DependencyGraph {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static final String DEPENDENCIES_PROPERTY = "dependencies";
	
	@JsonProperty(DEPENDENCIES_PROPERTY)
	private final Map<String, Collection<ModuleDescriptor>> dependencies;
	
	public Map<String, Collection<ModuleDescriptor>> getDependencies() {
		return dependencies;
	}

	@JsonCreator
	public DependencyGraph(final @JsonProperty(DEPENDENCIES_PROPERTY) Map<String, Collection<ModuleDescriptor>> dependencies
	) {
		final Set<Entry<String, Collection<ModuleDescriptor>>> entrySet = Preconditions.checkNotNull(dependencies).entrySet();
		final ImmutableMultimap.Builder<String, ModuleDescriptor> builder = ImmutableMultimap.builder();
		
		for (final Entry<String, Collection<ModuleDescriptor>> entry : entrySet) {
			final Collection<ModuleDescriptor> values = entry.getValue();
			for (final ModuleDescriptor moduleDescriptor : values) {
				Preconditions.checkState(moduleDescriptor instanceof ModuleDescriptor);
				builder.put(entry.getKey(), moduleDescriptor);
			}
		}
	
		this.dependencies = builder.build().asMap();;
	}
	
	public DependencyGraph(final SortedSetMultimap<String, ModuleDescriptor> dependencies
	) {
		this(dependencies.asMap());
	}

	@Override
	public int hashCode() {
		return dependencies.hashCode();
	}
	
	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof DependencyGraph)) {
			return false;
		}
		
		final DependencyGraph graph = (DependencyGraph) obj;
		
		return Objects.equal(this.dependencies, graph.dependencies);
	}
	
	public void write(final File file, final File prefixToTruncate) {
		final FileOutputStream outputStream;
		try {
			outputStream = new FileOutputStream(file);
		} catch (final FileNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		try {
			write(outputStream, prefixToTruncate);
		} finally {
			try {
				outputStream.close();
			} catch (IOException e) {
				
			}
		}
	}
	
	public <T extends OutputStream> T write(final T outputStream, final File prefixToTruncate) {
		try {
			
			if (prefixToTruncate == null) {
				OBJECT_MAPPER.writeValue(outputStream, this);
				return outputStream;
			}
			
			final ImmutableMultimap.Builder<String, ModuleDescriptor> multimapBuilder = ImmutableMultimap.builder();
			
			final Set<Entry<String, Collection<ModuleDescriptor>>> dependencies2 = this.getDependencies().entrySet();
			for (final Entry<String, Collection<ModuleDescriptor>> entry : dependencies2) {
				multimapBuilder.putAll(entry.getKey(), 
						Collections2.transform(entry.getValue(), new Function<ModuleDescriptor, ModuleDescriptor> () {

					@Override
					public ModuleDescriptor apply(final ModuleDescriptor paramF) {
						if (prefixToTruncate == null) {
							return paramF;
						}
						
						if (!paramF.getModuleFile().isPresent())
							return paramF;
						
						final ModuleDescriptor.Builder builder = new ModuleDescriptor.Builder();
						builder.copyOf(paramF).setModuleFile(
								new File(StringTransforms.trim
								(paramF.getModuleFile().get().getPath().replace(prefixToTruncate.getPath(), ""), File.separatorChar)));
						return builder.build();
					}
					
				}));
			}
			
			final DependencyGraph dependencyGraph = new DependencyGraph(multimapBuilder.build().asMap());
			OBJECT_MAPPER.writeValue(outputStream, dependencyGraph);
			return outputStream;
		} catch (final Exception e) {
			throw new RuntimeException(e);
		} 
	}
	
	public static DependencyGraph read(final InputStream inputStream) {
		try {
			return OBJECT_MAPPER.readValue(inputStream, DependencyGraph.class);
		} catch (final Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String toString() {
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		
		try {
			return write(outputStream, null).toString();
		} finally {
			try {
				outputStream.close();
			} catch (final IOException e) {}
		}
	}

}
