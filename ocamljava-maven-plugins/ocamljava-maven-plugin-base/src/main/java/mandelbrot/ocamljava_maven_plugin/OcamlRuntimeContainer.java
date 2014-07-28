package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

public class OcamlRuntimeContainer {

	private static final String OCAMLRUN_JAR = "ocamlrun.jar";

	private final AbstractMojo abstractMojo;
	
	private final Artifact runningArtifact;
	private final Artifact ocamlRuntime;
	private final File stagingFolder;

	private OcamlRuntimeContainer(final Builder builder) {
		this.abstractMojo = Preconditions.checkNotNull(builder.abstractMojo);
		this.ocamlRuntime = Preconditions.checkNotNull(builder.ocamlRuntime);
		this.runningArtifact = Preconditions.checkNotNull(builder.runningArtifact);
		this.stagingFolder = Preconditions.checkNotNull(builder.stagingFolder);
		init();
	}

	private void init() {
		
		final boolean madeDirs = this.stagingFolder.mkdirs();
		if (abstractMojo.getLog().isDebugEnabled())
			abstractMojo.getLog().debug("madeDirs ? " + madeDirs);
		
		
		final ImmutableSet<URL> set = ImmutableSet.<URL>builder()
			.add(copyOcamlRuntime())
			.add(copyRunningArtifact())
			.build();
		
		final URL[] urls = set.toArray(new URL[]{});
		
		final URLClassLoader urlClassLoader = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
		
		Thread.currentThread().setContextClassLoader(urlClassLoader);
	}

	private URL copyOcamlRuntime() throws RuntimeException {
		final File ocamlRunFile = new File(stagingFolder, OCAMLRUN_JAR);
		
		try {
			FileUtils.copyFile(ocamlRuntime.getFile(), ocamlRunFile);
			return ocamlRunFile.toURI().toURL();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}


	private URL copyRunningArtifact() throws RuntimeException {
		
		try {
			FileUtils.copyFileToDirectory(runningArtifact.getFile(), stagingFolder);
			return new File(stagingFolder, runningArtifact.getFile().getName()).toURI().toURL();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static class Builder {
		private AbstractMojo abstractMojo;
		private File stagingFolder;
		private Artifact ocamlRuntime;
		private Artifact runningArtifact;
		
		public Builder setOcamlRuntime(final Artifact artifact) {
			this.ocamlRuntime = artifact;
			return this;
		}
		
		public Builder setRunningArtifact(final Artifact artifact) {
			this.runningArtifact = artifact;
			return this;
		}
		
		public Builder setStagingFolder(final File folder) {
			this.stagingFolder = folder;
			return this;
		}
		public Builder setMojo(final AbstractMojo mojo) {
			this.abstractMojo = mojo;
			return this;
		}
		
		public OcamlRuntimeContainer build() {
			return new OcamlRuntimeContainer(this);
		}
	}
}
