package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.ocamljava_maven_plugin.io.UncheckedOutputStream;
import mandelbrot.ocamljava_maven_plugin.util.ClassPathGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.tools.ocamldep.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import com.google.common.base.Optional;
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
 * @since 1.0
 */
@Mojo(requiresProject=true, defaultPhase=LifecyclePhase.PROCESS_SOURCES, name="dep", executionStrategy="once-per-session", 
requiresDependencyResolution=ResolutionScope.RUNTIME, threadSafe=true)
public class OcamlJavaDependencyMojo extends OcamlJavaAbstractMojo {

	private static final String GOAL_NAME = OcamlJavaConstants.dependencyGoal();

	public static String fullyQualifiedGoal() {
		return GOAL_NAME;
	}
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		final Properties properties = System.getProperties();
		
		final URL[] classPathUrls = new ClassPathGatherer(this).getClassPathUrls(project, false);

		Thread.currentThread().setContextClassLoader(new 
				URLClassLoader(classPathUrls, Thread.currentThread().getContextClassLoader()));

		final Path path = Paths.get("").toAbsolutePath();
		
		final Object object = properties.get(FORK_PROPERTY_NAME);
		
		if (Boolean.parseBoolean(Optional.fromNullable(object).or(Boolean.TRUE).toString())) {
			getLog().info("[ocamldep] forking process");	
		
			final boolean forkAgain = false;
			invokePlugin(fullyQualifiedGoal(), forkAgain);
			
			final File prefixToTruncate = chooseOcamlSourcesDirectory();
			final DependencyGraph dependencyGraph = DependencyGraph.fromOcamlDep(rawDependencyTargetFullPath(), 
					prefixToTruncate);
		
			getLog().info("output directory to truncate with: " + project.getFile().getParent());
			dependencyGraph.write(chooseDependencyGraphTargetFullPath(), project.getFile().getParentFile());
		} else {
			getLog().info("[ocamldep] running in process");
			generateDependencyGraph();
		}
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
	
	/***
	 * Whether to sort the list of modules in dependency order.
	 */
	@Parameter(readonly=true, defaultValue="true")
	protected boolean sort = true;
		
	/***
	 * Only compile binaries for the java virtual machine (no *.cmo files).
	 */
	@Parameter(defaultValue="true")
	protected boolean javaOnly = true;
		
	/***
	 * Generate dependency information on all files.
	 */
	@Parameter(defaultValue="true", readonly=true)
	protected boolean all = true;
	 
	protected void generateDependencyGraph()
			throws MojoExecutionException {
		final Collection<String> ocamlSourceFiles = gatherOcamlSourceFiles(
				chooseOcamlSourcesDirectory()).values();

		final Collection<String> includePaths = FileMappings.buildPathMap(
				ocamlSourceFiles).keySet();

		final File dependencyGraphTargetFullPath = rawDependencyTargetFullPath();
		
		final boolean madeDirs = dependencyGraphTargetFullPath.getParentFile().mkdirs();
	
		if (getLog().isDebugEnabled())
			getLog().debug("made dirs? " + madeDirs);

		final UncheckedOutputStream<FileOutputStream> fileOutputStream = UncheckedOutputStream.fromFile(dependencyGraphTargetFullPath);

		
		final PrintStream printStream = new PrintStream(fileOutputStream);

		final List<String> commandLineArguments = generateCommandLineArguments(includePaths, ocamlSourceFiles);
		
		getLog().info("about to generate dependency graph: " + commandLineArguments);
		final ocamljavaMain main = mainWithReturn("ocamldep.jar",
				commandLineArguments
						.toArray(new String[] {}), printStream, ocamljavaMain.class);
		
		// This will never be invoked in practice because the process exits,
		// but if that ever changes this seems like it would be the proper thing to do
		checkForErrors("dependency generation error", main);
	}

	private File rawDependencyTargetFullPath() {
		return new File(chooseDependencyGraphTargetFullPath().getPath() + 
				".raw");
	}

}
;