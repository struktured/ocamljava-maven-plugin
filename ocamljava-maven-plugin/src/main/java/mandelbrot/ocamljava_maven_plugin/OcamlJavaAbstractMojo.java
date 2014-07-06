package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Properties;
import java.util.regex.Pattern;

import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.ocamljava_maven_plugin.util.FileGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.tools.ocamldep.ocamljavaMain;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.StringUtils;
import org.ocamljava.runtime.annotations.parameters.Parameters;
import org.ocamljava.runtime.kernel.AbstractNativeRunner;
import org.ocamljava.runtime.kernel.FalseExit;
import org.ocamljava.runtime.parameters.NativeParameters;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public abstract class OcamlJavaAbstractMojo extends AbstractMojo {

	private static final String OFFLINE_MODE = "-o";

	public static final String DEPENDENCIES_FILE_NAME = "dependencies.json";

	public static final String FORK_PROPERTY_NAME = "ocamljava.maven.plugin.fork";

	/***
	 * The name of the generated ocaml dependency graph file, to be place in the
	 * target directory (usually one of <code>target/ocaml-bin</code> or
	 * </code>target/ocaml-tests</code>)
	 * 
	 * @parameter default-value="dependencies.json"
	 * @required
	 */
	protected String dependencyGraphTarget = DEPENDENCIES_FILE_NAME;

	/***
	 * @parameter default-value="${project}"
	 * @required
	 * @readonly
	 */
	protected MavenProject project;

	/***
	 * The plugin descriptor.
	 * 
	 * @required
	 * @parameter default-value="${descriptor}"
	 */
	protected PluginDescriptor descriptor;

	/***
	 * Project's output directory, usually <code>target</code>.
	 * 
	 * @parameter property="project.build.directory"
	 * @required
	 */
	protected final File outputDirectory = new File("");

	/***
	 * The target subfolder to hold all compiled ocaml sources. This value is
	 * combined with the build output directory (usually <code>target</code>) to
	 * create an actual file path.
	 * 
	 * @parameter default-value="ocaml-bin"
	 */
	protected String ocamlCompiledSourcesTarget;

	/***
	 * Project's source directory.
	 * 
	 * @parameter default-value="src/main/ocaml"
	 */
	protected File ocamlSourceDirectory = new File("src/main/ocaml");

	/***
	 * The target jar to depend on and possibly replace with ocaml compiled
	 * sources depending on the value of the <code>replaceMainArtifact</code>
	 * parameter.
	 * 
	 * @parameter default-value="${project.artifactId}-${project.version}.jar"
	 * @required
	 */
	protected String targetJar;

	/***
	 * The target jar created by the ocamljava jar creation tool. If
	 * <code>replaceMainArtifact</code> is set to <code>true</code>, then this
	 * jar will replace the contents of the <code>targetJar</code> parameter.
	 * 
	 * @parameter 
	 *            default-value="${project.artifactId}-${project.version}-ocaml.jar"
	 * @required
	 */
	protected String targetOcamlJar;

	public String getOcamlCompiledSourcesTargetFullPath() {
		return outputDirectory.getPath() + File.separator
				+ chooseOcamlCompiledSourcesTarget();
	}

	protected abstract String chooseOcamlCompiledSourcesTarget();

	/***
	 * The target test jar to depend on and possibly replace with ocaml compiled
	 * sources depending on the value of the <code>replaceMainArtifact</code>
	 * parameter.
	 * 
	 * @parameter 
	 *            default-value="${project.artifactId}-${project.version}-tests.jar"
	 * @required
	 */
	protected String targetTestJar;

	/***
	 * The target test jar created by the ocamljava jar creation tool. If
	 * <code>replaceMainArtifact</code> is set to <code>true</code>, then this
	 * jar will replace the contents of the <code>targetTestJar</code>
	 * parameter.
	 * 
	 * @parameter 
	 *            default-value="${project.artifactId}-${project.version}-ocaml-tests.jar"
	 * @required
	 */
	protected String targetTestOcamlJar;

	/***
	 * The target subfolder to hold all compiled ocaml test sources. This value
	 * is combined with the build output directory (usually <code>target</code>)
	 * to create an actual file path.
	 * 
	 * @parameter default-value="ocaml-tests"
	 * 
	 */
	protected String ocamlCompiledTestsTarget;

	/***
	 * Project's source directory as specified in the POM.
	 * 
	 * @parameter default-value="src/test/ocaml"
	 */
	protected final File ocamlTestDirectory = new File("src/test/ocaml");

	/***
	 * Sets how java packages are determined for the code generated classes.
	 * <p>
	 * If set to <code>DYNAMIC</ocde>, a java package will be inferred according to the folder
	 * structure of the modules. For instance,
	 * <code>"src/main/ocaml/foo/bar/lib.ml"</code> will generate
	 * <code>package foo.bar</code> at the top of <code>LibWrapper.java</code>.</p><p>
	 * To fix the package name for all compiled module interfaces, set this
	 * value to <code>FIXED</code> and fill in the {@link #packageName}
	 * parameter accordingly.
	 * </p>
	 * 
	 * @parameter default-value="FIXED"
	 * 
	 **/
	protected JavaPackageMode javaPackageMode = JavaPackageMode.FIXED;

	public JavaPackageMode getJavaPackageMode() {
		return javaPackageMode;
	}

	public void setJavaPackageMode(JavaPackageMode javaPackageMode) {
		this.javaPackageMode = javaPackageMode;
	}

	public static enum JavaPackageMode {
		FIXED {
			@Override
			public String choosePackage(final String dynamicName, final String staticName) {
				return staticName;
			}
		}, DYNAMIC {
			@Override
			public String choosePackage(final String dynamicName, final String staticName) {
				return dynamicName;
			}
		};

		public static JavaPackageMode getDefaultValue() {
			return DYNAMIC;
		}

		public abstract String choosePackage(String dynamicName, String staticName);
	}

	/***
	 * Sets the java package name for each source file.
	 * 
	 * @parameter default-value=""
	 * 
	 **/
	protected String packageName;

	protected String toPackage(final File prefixToTruncate, final String path) {

		if (isDynamicPackageMode()) {
			return FileMappings.toPackage(prefixToTruncate, path);
		} else {
			return packageName;
		}
	}

	public boolean isDynamicPackageMode() {
		return JavaPackageMode.DYNAMIC.equals(javaPackageMode);
	}

	/***
	 * Whether to enable extensions that allow ocaml modules to access plain
	 * java objects.
	 * 
	 * @parameter default-value="true"
	 * 
	 */
	protected boolean javaExtensions;

	protected Multimap<String, String> gatherOcamlSourceFiles(final File root) {
		return new FileGatherer(this).gatherFiles(root,
				OcamlJavaConstants.OCAML_SOURCE_FILE_EXTENSIONS);
	}

	protected void addIncludePaths(final Collection<String> includePaths,
			final ImmutableCollection.Builder<String> builder) {
		for (final String includePath : includePaths) {
			if (!StringUtils.isBlank(includePath)) {
				builder.add(OcamlJavaConstants.INCLUDE_DIR_OPTION).add(
						includePath);
			}
		}
	}

	protected void checkForErrors(final String message,
			final AbstractNativeRunner main) throws MojoExecutionException {
		final Field declaredField;
		try {
			declaredField = getExceptionField();
		} catch (final NoSuchFieldException e) {
			throw new MojoExecutionException(message, e);
		}
		final boolean accessible = declaredField.isAccessible();
		try {
			declaredField.setAccessible(true);
			final Throwable exception = (Throwable) declaredField.get(main);

			if (exception != null) {
				if (exception instanceof FalseExit) {
					final FalseExit f = (FalseExit) exception;
					switch (f.getExitCode()) {
					case 0:
						break;
					default:
						throw new MojoExecutionException(message
								+ " (exit code = " + f.getExitCode() + ")");
					}
				} else
					throw new MojoExecutionException(message, exception);

			}

		} catch (final MojoExecutionException e) {
			throw e;
		} catch (final Exception e) {
			throw new MojoExecutionException(message, e);
		} finally {
			declaredField.setAccessible(accessible);
			main.clearException();
		}
	}

	// This seems to be only the way to access the exception protected field
	// from the ocaml main object at this time.
	private static Field getExceptionField() throws NoSuchFieldException {
		return AbstractNativeRunner.class.getDeclaredField("exception");
	}

	public File chooseDependencyGraphTargetFullPath() {
		return new File(getOcamlCompiledSourcesTargetFullPath()
				+ File.separator + dependencyGraphTarget);
	}

	protected DependencyGraph getDependendyGraph(
			final Multimap<String, String> filesByExtension)
			throws MojoExecutionException {
		final FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(new File(
					Collections2
							.filter(filesByExtension.get(OcamlJavaConstants.JSON_EXTENSION),
									dependencyGraphFileExists())
							.iterator().next()));
		} catch (final FileNotFoundException e) {
			throw new MojoExecutionException(
					"missing or corrupt dependency graph: "
							+ dependencyGraphTarget + ", can't wrap!", e);
		}

		final DependencyGraph dependencyGraph = DependencyGraph
				.read(inputStream);
		return dependencyGraph;
	}

	protected Predicate<CharSequence> dependencyGraphFileExists() {
		return Predicates.contains(Pattern
				.compile(dependencyGraphTarget
						.replace(".", "\\.")));
	}
	

	protected static ocamljavaMain mainWithReturn(final String jarName,
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
							false, jarName, ocamljavaMain.class));
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


	/***
	 * <p>Invokes a goal on a separate process programmatically using the Maven invoker tool.</p>
	 * @param goal the maven goal, such as <code>jar:package</code> or <code>mandebrlot:ocamljava-maven-plugin:compile</code>.
	 * @param forkAgain sets a system property ({@value #FORK_PROPERTY_NAME}} as a hint to the invoking process on whether it should fork once more.   
	 * @return the invocation result.
	 * @throws MojoExecutionException if an invocation exception occurs, or the invoked process did not exit with a return value of 0.
	 */
	protected InvocationResult invokePlugin(final String goal, final boolean forkAgain) throws MojoExecutionException {
		return invokePlugin(goal, forkAgain, null);
	}
	
	/***
	 * <p>Invokes a goal on a separate process programmatically using the Maven invoker tool.</p>
	 * @param goal the maven goal, such as <code>jar:package</code> or <code>mandebrlot:ocamljava-maven-plugin:compile</code>.
	 * @param forkAgain sets a system property ({@value #FORK_PROPERTY_NAME}} as a hint to the invoking process on whether it should fork once more.   
	 * @param properties system properties to pass to the Maven command.
	 * @return the invocation result.
	 * @throws MojoExecutionException if an invocation exception occurs, or the invoked process did not exit with a return value of 0.
	 */
	protected InvocationResult invokePlugin(final String goal, final boolean forkAgain, Properties properties) throws MojoExecutionException {

		properties = properties == null ? new Properties() : (Properties)properties.clone();
		
		properties.put(FORK_PROPERTY_NAME, Boolean.valueOf(forkAgain).toString());

		final InvocationRequest defaultInvocationRequest = new DefaultInvocationRequest()
				.setDebug(getLog().isDebugEnabled())
				.setMavenOpts(System.getenv("MAVEN_OPTS"))
				.setGoals(ImmutableList.of(goal))				
				.setProperties(properties)
				.setOffline(isOffline())
				.setPomFile(project.getFile());
				
		final Invoker invoker = new DefaultInvoker();
		try {
			final InvocationResult execution = invoker
					.execute(defaultInvocationRequest);
			switch (execution.getExitCode()) {
			case 0:
				return execution;
			default:
				throw new MojoExecutionException(
						"process did not exit cleanly (exit code = " + execution.getExitCode() + ")");
			}

		} catch (final MavenInvocationException e) {
			throw new MojoExecutionException("problem during fork operation", e);
		}
	}

	protected boolean isOffline() {
		final Optional<String> optional = Optional.fromNullable(System.getProperty("sun.java.command")); 
		if (optional.isPresent()) {
			return ImmutableSet.copyOf(Splitter.on(" ").split(optional.get())).contains(OFFLINE_MODE);
		}
		return false;
	}
}
