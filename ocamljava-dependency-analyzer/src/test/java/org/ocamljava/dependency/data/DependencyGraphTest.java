package org.ocamljava.dependency.data;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;
import org.ocamljava.dependency.analyzer.SharedTestInstances;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.collect.SortedSetMultimap;

public class DependencyGraphTest {

	@Test
	public void shouldSerializeAsJson() {
		
		final SortedSetMultimap<String,ModuleDescriptor> simpleDependency = SharedTestInstances.createSimpleDependency();
		final DependencyGraph expectedGraph = new DependencyGraph(simpleDependency);
	
		final String readValue = expectedGraph.toString();
		
		final DependencyGraph actualGraph = DependencyGraph.read(new ByteArrayInputStream(readValue.getBytes()));
	
		final Map<String, Collection<ModuleDescriptor>> dependencies = expectedGraph.getDependencies();
		final Map<String, Collection<ModuleDescriptor>> dependencies2 = actualGraph.getDependencies();
		
		final Set<Entry<String,Collection<ModuleDescriptor>>> entrySet = ImmutableMap.copyOf(dependencies).entrySet();
		for (final Entry<String, Collection<ModuleDescriptor>> entry : entrySet) {
			final String key = entry.getKey();
			final Collection<ModuleDescriptor> collection = dependencies.get(key);
			final Collection<ModuleDescriptor> collection2 = dependencies2.get(key);
			
			for (final ModuleDescriptor moduleDescriptor : collection) {
				Assert.assertTrue(collection2.contains(moduleDescriptor));
			}
			Assert.assertEquals(collection.size(), collection2.size());
		} 
 

		final Collection<Collection<ModuleDescriptor>> values = dependencies2.values();
		
		for (final Collection<ModuleDescriptor> collection : values) {
			for (final ModuleDescriptor moduleDescriptor : collection) {
				Assert.assertTrue(moduleDescriptor instanceof ModuleDescriptor);
			}
		}
		
		//final Set<Entry<String, Collection<ModuleDescriptor>>> difference = Sets.difference(dependencies.values(), dependencies2.values());
		//Assert.assertEquals(ImmutableSet.copyOf(dependencies.values()), ImmutableSet.copyOf(dependencies2.values()));
		//Asert.assertTrue("difference is not empty: " + difference, difference.isEmpty());
		
		Assert.assertEquals(expectedGraph, actualGraph);
		
	}

}
