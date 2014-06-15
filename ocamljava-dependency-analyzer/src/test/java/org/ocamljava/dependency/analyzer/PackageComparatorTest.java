package org.ocamljava.dependency.analyzer;

import junit.framework.Assert;
import mandelbrot.dependency.analyzer.PackageComparator;
import mandelbrot.dependency.data.ModuleDescriptor;

import org.junit.Test;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;

import static org.ocamljava.dependency.analyzer.SharedTestInstances.*;

public class PackageComparatorTest {


	@Test
	public void shouldOrderTwoPackages() {
		
		final SortedSetMultimap<String, ModuleDescriptor> dependencies = SharedTestInstances.createSimpleDependency();
		final PackageComparator packageComparator = new PackageComparator(dependencies);
		
		final String packageName = COM_FIRST;
		final String packageName2 = COM_SECOND;
		
		Assert.assertEquals(-1, packageComparator.compare(packageName, packageName2));
		Assert.assertEquals(1, packageComparator.compare(packageName2, packageName));
		Assert.assertEquals(0, packageComparator.compare(packageName, packageName));
		
	}
	
	@Test
	public void shouldOrderAlphabetically() {
		final String packageName = "com.alpha";
		final String packageName2 = "com.beta";
		
		final PackageComparator packageComparator = new PackageComparator(TreeMultimap.<String, ModuleDescriptor>create());
		
		Assert.assertEquals(-1, packageComparator.compare(packageName, packageName2));
		Assert.assertEquals(1, packageComparator.compare(packageName2, packageName));
		Assert.assertEquals(0, packageComparator.compare(packageName, packageName));
		
	}
	
	
}