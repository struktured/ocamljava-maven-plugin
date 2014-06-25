package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.util.Collection;
import java.util.List;

import mandelbrot.ocamljava_maven_plugin.util.ClassPathGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.tools.ocamldep.ocamljavaConstants;
import ocaml.tools.ocamldep.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

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
		
		final ocamljavaMain main = 
				ocamljavaMain.mainWithReturn(generateCommandLineArguments(includePaths, ocamlSourceFiles).toArray(new String[] {}));
	
		checkForErrors("ocamljava dependency resolution failed", main);
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
