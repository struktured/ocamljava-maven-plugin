package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.util.Collection;
import java.util.List;

import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.tools.camlp4.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.common.collect.ImmutableList;

/**
 * <p>
 * This is a goal which executes the campl4 preprocessing tool during the
 * process-sources phase. It is the same as executing something like
 * </p>
 * <p>
 * <code>camlp4 -I com/foobar foo.ml bar.ml ...</code>
 * </p>
 * from the command line but instead uses maven properties to infer the source
 * locations and class path. All parameters can be overridden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @since 1.0
 */
@Mojo(requiresProject = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES, name = "camlp4", executionStrategy = "once-per-session", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class Camlp4Mojo extends OcamlJavaAbstractMojo {

	// -parser <name> Load the parser Camlp4Parsers/<name>.cm(o|a|xs)
	public static final String PARSER_OPTION = "-parser";
	// -printer <name> Load the printer Camlp4Printers/<name>.cm(o|a|xs)
	public static final String PRINTER_OPTION = "-printer";
	// -filter <name> Load the filter Camlp4Filters/<name>.cm(o|a|xs
	public static final String FILTER_OPTION = "-filter";

	private static final String GOAL_NAME = OcamlJavaConstants.camlp4Goal();

	public static String fullyQualifiedGoal() {
		return GOAL_NAME;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		runInProcess();
	}

	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

	private List<String> generateCommandLineArguments(
			final Collection<String> includePaths,
			final Collection<String> ocamlSourceFiles)
			throws MojoExecutionException {

		final ImmutableList.Builder<String> builder = ImmutableList
				.<String> builder();

		if (verbose)
			builder.add(OcamlJavaConstants.VERBOSE_OPTION);

		addIncludePaths(includePaths, builder);

		builder.addAll(ocamlSourceFiles);

		return builder.build();
	}

	protected void runInProcess() throws MojoExecutionException {
		final Collection<String> ocamlSourceFiles = gatherOcamlSourceFiles(
				chooseOcamlSourcesDirectory()).values();

		final Collection<String> includePaths = FileMappings.buildPathMap(
				ocamlSourceFiles).keySet();

		final List<String> commandLineArguments = generateCommandLineArguments(
				includePaths, ocamlSourceFiles);

		if (getLog().isDebugEnabled())
			getLog().debug("[camlp4] " + commandLineArguments);

		ocamljavaMain.main(commandLineArguments.toArray(new String[] {}));

	}

	/***
	 * Whether to enable verbosity mode.
	 */
	@Parameter(defaultValue = "false")
	protected boolean verbose;

	
};
