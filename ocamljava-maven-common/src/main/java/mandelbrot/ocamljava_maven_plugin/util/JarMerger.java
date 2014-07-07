package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.maven.plugin.AbstractMojo;

import com.google.common.base.Preconditions;

public class JarMerger {
	private final AbstractMojo abstractMojo;

	public JarMerger(final AbstractMojo abstractMojo) {
		this.abstractMojo = Preconditions.checkNotNull(abstractMojo);
	}
	
	public void merge(final File mergeFromJar, final File mergeToJar) throws IOException {
		
		final Collection<EntryInfo> entryInfos = new JarEntryReader(abstractMojo).readEntries(mergeFromJar.getPath());
		final JarAppender jarAppender = new JarAppender(abstractMojo);
		
		jarAppender.appendEntries(entryInfos, mergeFromJar.getPath());
	}

}
