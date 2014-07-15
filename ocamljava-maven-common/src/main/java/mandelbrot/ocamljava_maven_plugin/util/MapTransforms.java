package mandelbrot.ocamljava_maven_plugin.util;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.*;
import com.google.common.base.*;

public class MapTransforms {

	public static <T> Map<T, T> reverseIndex(
			final Multimap<T, T> filesByPackageName) {
		final Map<T, Collection<T>> asMap = filesByPackageName
				.asMap();
		final Set<Entry<T, Collection<T>>> entrySet = asMap
				.entrySet();
		final ImmutableMap.Builder<T, T> immutableMapBuilder = ImmutableMap
				.builder();

		for (final Entry<T, Collection<T>> entry : entrySet) {
			for (final T value : entry.getValue()) {
				immutableMapBuilder.put(value, entry.getKey());
			}
		}
		return immutableMapBuilder.build();
	}
}
