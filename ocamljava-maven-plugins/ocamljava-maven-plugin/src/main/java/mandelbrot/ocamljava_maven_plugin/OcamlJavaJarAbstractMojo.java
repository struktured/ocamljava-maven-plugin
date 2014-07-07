package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.util.Collection;

import mandelbrot.ocamljava_maven_plugin.util.FilesByExtensionGatherer;
import mandelbrot.ocamljava_maven_plugin.util.JarAppender;
import ocaml.compilers.ocamljavaMain;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public abstract class OcamlJavaJarAbstractMojo extends OcamlJavaAbstractMojo {
	
	/***
	 * Whether to replace the main artifact jar with ocaml enhanced version.
	 * @parameter default-value="true"
	 */
	protected boolean replaceMainArtfact = true;
	
	protected abstract String chooseTargetJar();

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (!ensureTargetDirectoryExists()) {
			getLog().error("Could not create target directory");
			return;
		}
		
		try {

			final File root = new File(getOcamlCompiledSourcesTargetFullPath());
			final Multimap<String, String> ocamlCompiledSourceFiles = new FilesByExtensionGatherer(
					this, ImmutableSet.of(
							OcamlJavaConstants.COMPILED_IMPL_JAVA_EXTENSION,
							OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION))
					.gather(root);

			if (ocamlCompiledSourceFiles.isEmpty()) {
				getLog().info("No sources to add to jar (scanned directory: " + root.getPath() + ")");
				return;
			}
			
			final String[] args = generateCommandLineArguments(ocamlCompiledSourceFiles.get(
					OcamlJavaConstants.COMPILED_IMPL_JAVA_EXTENSION)).toArray(new String[]{});

			getLog().info("args: " + ImmutableList.copyOf(args));
			
			final ocamljavaMain mainWithReturn = ocamljavaMain.mainWithReturn(args);
		
			checkForErrors("ocaml java jar execution failed", mainWithReturn);
			
			if (attachCompiledModules) {
				
				final Collection<String> compiledModuleInterfaces =
					ocamlCompiledSourceFiles.get(OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION);
				
				final Collection<String> all = ImmutableList.<String>builder()
						.add(chooseDependencyGraphTargetFullPath().getPath())
						.addAll(compiledModuleInterfaces)
						.addAll(ocamlCompiledSourceFiles.get(OcamlJavaConstants.COMPILED_IMPL_OCAML_EXTENSION))
						.addAll(ocamlCompiledSourceFiles.get(OcamlJavaConstants.COMPILED_IMPL_JAVA_EXTENSION)).build();
				
				new JarAppender(this).addFiles(getTargetOcamlJarFullPath(), all, 
						getOcamlCompiledSourcesTargetFullPath());
				
			}
			
			if (replaceMainArtfact) {
				getLog().info(("replacing main artifact " + getTargetJarFullPath() + " with " + getTargetOcamlJarFullPath()));
				FileUtils.copyFile(new File(getTargetOcamlJarFullPath()), 
						new File(getTargetJarFullPath()));
			}
			
			
		} catch (final Exception e) {
			throw new MojoExecutionException("ocamljava threw an error", e);
		}
	}


	

	public String getTargetJarFullPath() {
		return outputDirectory.getPath() + File.separator + chooseTargetJar();
	}
	
	public String getTargetOcamlJarFullPath() {
		return outputDirectory.getPath() + File.separator + chooseTargetOcamlJar();
	}


	/***
	 * Determines whether to attach the compiled modules and module interfaces to the final packaged jar.
	 * This is necessary for projects that will invoke the ocaml wrap goal and reference this artifact.
	 * @parameter default-value="true"
	 */
	protected boolean attachCompiledModules;
	
	private ImmutableList<String> generateCommandLineArguments(
			final Collection<String> ocamlSourceFiles) {
		return ImmutableList.<String>builder()
				.add(OcamlJavaConstants.ADD_TO_JAR_SOURCES_OPTION)
				.add(getTargetOcamlJarFullPath())
				.add(OcamlJavaConstants.ADDITIONAL_JAR_OPTION)
				.add(getTargetJarFullPath())
				.addAll(ocamlSourceFiles)
				.build();
	}
	
	private boolean ensureTargetDirectoryExists() {
		if (outputDirectory.exists()) {
			return true;
		}
		return outputDirectory.mkdirs();
	}

	protected abstract String chooseTargetOcamlJar();
	
}
