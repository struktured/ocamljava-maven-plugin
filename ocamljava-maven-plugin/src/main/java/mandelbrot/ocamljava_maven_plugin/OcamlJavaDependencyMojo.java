package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;

import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.tools.ocamldep.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.ocamljava.runtime.annotations.parameters.Parameters;
import org.ocamljava.runtime.parameters.NativeParameters;

import com.google.common.collect.ImmutableList;

/**
 * <p>
 * This is a goal which anaylzes OCaml sources during the maven process sources
 * phase to determine the build dependency order. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamldep -I com/foobar foo.ml bar.ml ...</code>
 * </p>
 * from the command line but instead uses maven properties to infer the source
 * locations and class path. All parameters can be overridden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @requiresProject
 * @goal dep
 * @phase process-sources
 * @executionStrategy once-per-session
 * @requiresDependencyResolution runtime
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaDependencyMojo extends OcamlJavaAbstractMojo {

	
	/***
	 * Whether to sort the list of modules in dependency order.
	 * @parameter default-value="true"
	 */
	protected boolean sort = true;
		
	/***
	 * Only compile binaries for the java virtual machine (no *.cmo files).
	 * @parameter default-value="true"
	 */
	protected boolean javaOnly = true;
		
	/***
	 * Generate dependency information on all files.
	 * @parameter default-value="true"
	 */
	protected boolean all = true;
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		final Collection<String> ocamlSourceFiles = gatherOcamlSourceFiles(chooseOcamlSourcesDirectory()).values();
		
		final Collection<String> includePaths = FileMappings.buildPathMap(ocamlSourceFiles).keySet();
		
		final FileOutputStream outputStream;
		try {
			File chooseDependencyGraphTargetFullPath = chooseDependencyGraphTargetFullPath();
			
			final boolean madeDirs = chooseDependencyGraphTargetFullPath.getParentFile().mkdir();
		
			if (getLog().isDebugEnabled())
				getLog().info("made dirs? " + madeDirs);
			
			outputStream = new FileOutputStream(chooseDependencyGraphTargetFullPath);
		} catch (final FileNotFoundException e) {
			throw new MojoExecutionException("file error", e);
		}
		final PrintStream printStream = new PrintStream(outputStream);
		
		final ocamljavaMain main = 
				mainWithReturn(generateCommandLineArguments(includePaths, ocamlSourceFiles).toArray(new String[] {}), printStream);
		
		printStream.close();
		checkForErrors("ocamljava dependency resolution failed", main);
		
		
	}

	private static ocamljavaMain mainWithReturn(
			java.lang.String[] paramArrayOfString, final PrintStream out) throws MojoExecutionException {
		final ocamljavaMain ocamljavaMain;
		try {
			final Constructor<ocaml.tools.ocamldep.ocamljavaMain> declaredConstructor = ocamljavaMain.class.getDeclaredConstructor(
					NativeParameters.class);
			final boolean accessible = declaredConstructor.isAccessible();
			if (!accessible)
				declaredConstructor.setAccessible(true);
			ocamljavaMain = declaredConstructor.newInstance(
					Parameters.fromStream(ocamljavaMain.class
							.getResourceAsStream("ocamljava.parameters"),
							paramArrayOfString, System.in, out, System.err,
							false, "ocamldep.jar", ocamljavaMain.class));
			if (!accessible)
				declaredConstructor.setAccessible(false);
		} catch (final Exception e) {
			throw new MojoExecutionException("error creating main instance", e);
		}
		ocamljavaMain.execute();
		return ocamljavaMain;
	}

	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	private List<String> generateCommandLineArguments(
			final Collection<String> includePaths,
			final Collection<String> ocamlSourceFiles)
			throws MojoExecutionException {

		final ImmutableList.Builder<String> builder = ImmutableList
				.<String> builder();

		
		if (javaOnly)
			builder.add(OcamlJavaConstants.JAVA_ONLY_OPTION);
		
		if (sort)
			builder.add(OcamlJavaConstants.SORT_OPTION);
		
		if (all)
			builder.add((OcamlJavaConstants.ALL_OPTION));
		
		addIncludePaths(includePaths, builder);
	
		builder.addAll(ocamlSourceFiles);
		
		
		return builder.build();
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

}
