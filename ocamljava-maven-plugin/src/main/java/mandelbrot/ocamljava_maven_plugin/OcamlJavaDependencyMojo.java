package mandelbrot.ocamljava_maven_plugin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.tools.ocamldep.ocamljavaMain;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

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
 * @requiresProject
 * @goal dep
 * @phase process-sources
 * @executionStrategy once-per-session
 * @requiresDependencyResolution runtime
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaDependencyMojo extends OcamlJavaAbstractMojo {

	public static final String FORK_PROPERTY_NAME = "ocamljava.plugin.fork";
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		
		Properties properties = (Properties) System.getProperties().clone();
	
		final DefaultInvocationRequest defaultInvocationRequest = new DefaultInvocationRequest();
	
		final Object object = properties.get(FORK_PROPERTY_NAME);
		
		if (Boolean.parseBoolean(Optional.fromNullable(object).or(Boolean.TRUE).toString())) {
			getLog().info("forking process");	
			
			properties = new Properties();
			properties.put(FORK_PROPERTY_NAME, Boolean.FALSE.toString());
		
			defaultInvocationRequest				
				.setDebug(getLog().isDebugEnabled())
				.setMavenOpts(System.getenv("MAVEN_OPTS"))
				.setGoals(ImmutableList.of("mandelbrot:ocamljava-maven-plugin:dep"))
				.setProperties(properties)
				.setPomFile(project.getFile());
				
			final Invoker invoker = new DefaultInvoker();	
			try {
				final InvocationResult execute = invoker.execute(defaultInvocationRequest);
				getLog().info("result: " + execute);
			} catch (final MavenInvocationException e) {
				throw new MojoExecutionException("problem during fork operation", e);
			}
			
		} else {
			getLog().info("running in process");
			generateDependencyGraph();
		}
	}

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
	 * @parameter default-value="true"
	 * @readonly
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
	 * @readonly
	 */
	protected boolean all = true;
	 
	protected DependencyGraph generateDependencyGraph()
			throws MojoExecutionException {
		final Collection<String> ocamlSourceFiles = gatherOcamlSourceFiles(
				chooseOcamlSourcesDirectory()).values();

		final Collection<String> includePaths = FileMappings.buildPathMap(
				ocamlSourceFiles).keySet();

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final File chooseDependencyGraphTargetFullPath = chooseDependencyGraphTargetFullPath();

		final boolean madeDirs = chooseDependencyGraphTargetFullPath
				.getParentFile().mkdir();

		if (getLog().isDebugEnabled())
			getLog().debug("made dirs? " + madeDirs);

		final PrintStream printStream = new PrintStream(outputStream);

		getLog().info("about to generate dependency graph");
		final ocamljavaMain main = mainWithReturn("ocamldep.jar",
				generateCommandLineArguments(includePaths, ocamlSourceFiles)
						.toArray(new String[] {}), printStream);

		getLog().info("finished to generat dependency graph");

		try {
			final FileOutputStream fileOutputStream = new FileOutputStream(
					chooseDependencyGraphTargetFullPath);
			final DependencyGraph dependencyGraph = DependencyGraph
					.fromOcamlDep(outputStream, chooseOcamlSourcesDirectory());
			dependencyGraph.write(fileOutputStream,
					chooseOcamlSourcesDirectory());
			fileOutputStream.close();
			checkForErrors("ocamljava dependency resolution failed", main);
			return dependencyGraph;
		} catch (final Exception e) {
			throw new MojoExecutionException("error writing dependency info", e);
		} finally {
			printStream.close();
		}
	}

}
