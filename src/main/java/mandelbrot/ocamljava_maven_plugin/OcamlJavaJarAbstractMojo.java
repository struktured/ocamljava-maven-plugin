package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.util.Collection;

import mandelbrot.ocamljava_maven_plugin.util.FilesByExtensionGatherer;
import mandelbrot.ocamljava_maven_plugin.util.JarAppender;
import ocaml.compilers.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public abstract class OcamlJavaJarAbstractMojo extends OcamlJavaAbstractMojo {

	protected abstract String chooseTargetJar();

	@Override
	public void execute() throws MojoExecutionException {

		if (!ensureTargetDirectoryExists()) {
			getLog().error("Could not create target directory");
			return;
		}
		
		try {

			final Multimap<String, String> ocamlCompiledSourceFiles = new FilesByExtensionGatherer(
					this, ImmutableSet.of(
							OcamlJavaConstants.COMPILED_IMPL_EXTENSION,
							OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION))
					.gather(new File(getOcamlCompiledSourcesTargetFullPath()));
		
			final String[] args = generateCommandLineArguments(targetJar, ocamlCompiledSourceFiles.get(
					OcamlJavaConstants.COMPILED_IMPL_EXTENSION)).toArray(new String[]{});

			getLog().info("args: " + ImmutableList.copyOf(args));
			
			@SuppressWarnings("unused")
			final ocamljavaMain mainWithReturn = ocamljavaMain.mainWithReturn(args);
			
			if (attachCompiledModuleInterfaces) {
				
				final Collection<String> compiledModuleInterfaces =
					ocamlCompiledSourceFiles.get(OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION);
				new JarAppender(this).addFiles(getTargetJarFullPath(), compiledModuleInterfaces);
			}
			
		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}


	/***
	 * Determines whether to attach the compiled module interfaces to the final packaged jar.
	 * @parameter default-value="true"
	 */
	protected boolean attachCompiledModuleInterfaces;
	
	private ImmutableList<String> generateCommandLineArguments(final String targetJar,
			final Collection<String> ocamlSourceFiles) {
		return ImmutableList.<String>builder().add(OcamlJavaConstants.ADD_TO_JAR_SOURCES_OPTION).
				add(getTargetJarFullPath()).addAll(ocamlSourceFiles).build();
	}
	
	private boolean ensureTargetDirectoryExists() {
		if (outputDirectory.exists()) {
			return true;
		}
		return outputDirectory.mkdirs();
	}
	
}
