package mandelbrot.ocamljava_maven_plugin;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import mandelbrot.dependency.data.DependencyGraph;
import mandelbrot.ocamljava_maven_plugin.io.UncheckedOutputStream;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.compilers.Ccomp;
import ocaml.compilers.Clflags;
import ocaml.compilers.Compenv;
import ocaml.compilers.Config;
import ocaml.compilers.Lexer;
import ocaml.compilers.Location;
import ocaml.compilers.Longident;
import ocaml.compilers.Misc;
import ocaml.compilers.Parse;
import ocaml.compilers.Parser;
import ocaml.compilers.Pparse;
import ocaml.compilers.Syntaxerr;
import ocaml.compilers.Terminfo;
import ocaml.compilers.Warnings;
import ocaml.stdlib.Arg;
import ocaml.stdlib.Array;
import ocaml.stdlib.Buffer;
import ocaml.stdlib.CamlinternalLazy;
import ocaml.stdlib.Char;
import ocaml.stdlib.Digest;
import ocaml.stdlib.Filename;
import ocaml.stdlib.Format;
import ocaml.stdlib.Hashtbl;
import ocaml.stdlib.Int32;
import ocaml.stdlib.Int64;
import ocaml.stdlib.Lexing;
import ocaml.stdlib.Marshal;
import ocaml.stdlib.Nativeint;
import ocaml.stdlib.Obj;
import ocaml.stdlib.Parsing;
import ocaml.stdlib.Pervasives;
import ocaml.stdlib.Printf;
import ocaml.stdlib.Random;
import ocaml.stdlib.Set;
import ocaml.stdlib.Sys;
import ocaml.tools.ocamldep.Ocamldep;
import ocaml.tools.ocamldep.ocamljavaMain;
import ocaml.tools.ocamldoc.Depend;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.shared.invoker.InvocationOutputHandler;
import org.codehaus.plexus.component.configurator.converters.basic.ByteConverter;
import org.codehaus.plexus.util.StringOutputStream;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import fr.x9c.barista.ByteBuffer;

/**
 * <p>
 * This is a goal which anaylzes OCaml sources during the maven process sources
 * phase to determine the build dependency order. It is the same as executing
 * something like
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
@Mojo(requiresProject = true, defaultPhase = LifecyclePhase.PROCESS_SOURCES, name = "dep", executionStrategy = "once-per-session", requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class OcamlJavaDependencyMojo extends OcamlJavaAbstractMojo {

	private static final String GOAL_NAME = OcamlJavaConstants.dependencyGoal();

	public static String fullyQualifiedGoal() {
		return GOAL_NAME;
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		final Properties properties = System.getProperties();

		// final URL[] classPathUrls = new
		// ClassPathGatherer(this).getClassPathUrls(project, false);
		//
		// Thread.currentThread().setContextClassLoader(new
		// URLClassLoader(classPathUrls,
		// Thread.currentThread().getContextClassLoader()));
		//
		// final Path path = Paths.get("").toAbsolutePath();

		final Object object = properties.get(FORK_PROPERTY_NAME);

		if (Boolean.parseBoolean(Optional.fromNullable(object).or(Boolean.TRUE)
				.toString())) {
			getLog().info("[ocamldep] forking process");

			final File path = this.rawDependencyTargetFullPath();
			
			path.getParentFile().mkdirs();
			try {
				path.createNewFile();
			} catch (final Exception e) {
				throw new MojoExecutionException("couldn't create target file", e);
			}
			
			final UncheckedOutputStream<FileOutputStream> fileOutputStream = UncheckedOutputStream.fromFile(path);
			
			
			final boolean forkAgain = false;
			final File prefixToTruncate = chooseOcamlSourcesDirectory();
			final InvocationOutputHandler handler = new InvocationOutputHandler() {
				
				@Override
				public void consumeLine(final String line) {
					if (line.startsWith("["))
						return;
					
					if (!line.contains(prefixToTruncate.getAbsolutePath())) {
						return;
					}
					
					fileOutputStream.write((line+  System.getProperty("line.separator")).getBytes());
				}
			};
			
			invokePlugin(fullyQualifiedGoal(), forkAgain, handler);

			fileOutputStream.close();
				
			final DependencyGraph dependencyGraph = DependencyGraph
					.fromOcamlDep(rawDependencyTargetFullPath(),
							prefixToTruncate);

			getLog().info(
					"output directory to truncate with: "
							+ project.getFile().getParent());
			dependencyGraph.write(chooseDependencyGraphTargetFullPath(),
					project.getFile().getParentFile());
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
	@Parameter(readonly = true, defaultValue = "true")
	protected boolean sort = true;

	/***
	 * Only compile binaries for the java virtual machine (no *.cmo files).
	 */
	@Parameter(defaultValue = "true")
	protected boolean javaOnly = true;

	/***
	 * Generate dependency information on all files.
	 */
	@Parameter(defaultValue = "true", readonly = true)
	protected boolean all = true;

	protected void generateDependencyGraph() throws MojoExecutionException {
		final Collection<String> ocamlSourceFiles = gatherOcamlSourceFiles(
				chooseOcamlSourcesDirectory()).values();

		final Collection<String> includePaths = FileMappings.buildPathMap(
				ocamlSourceFiles).keySet();

		final File dependencyGraphTargetFullPath = rawDependencyTargetFullPath();

		final boolean madeDirs = dependencyGraphTargetFullPath.getParentFile()
				.mkdirs();

		if (getLog().isDebugEnabled())
			getLog().debug("made dirs? " + madeDirs);

		//final UncheckedOutputStream<FileOutputStream> fileOutputStream = UncheckedOutputStream
	//			.fromFile(dependencyGraphTargetFullPath);

//		final PrintStream printStream = new PrintStream(fileOutputStream);

		final List<String> commandLineArguments = generateCommandLineArguments(includePaths, ocamlSourceFiles);

		if (getLog().isDebugEnabled())
			getLog().debug(
					"about to generate dependency graph: "
							+ commandLineArguments);
		ocamljavaMain.main(commandLineArguments.toArray(new String[]{}));
//		final ocamljavaMain ocamljavaMain = mainWithReturn("ocamldep.jar",
//				commandLineArguments.toArray(new String[] {}), printStream,
//				ocamljavaMain.class),
//				new Function<ocamljavaMain, ocamljavaMain>() {
//
//					@Override
//					public ocamljavaMain apply(final ocamljavaMain ocamljavaMain) {
//
//						getLog().info("inside apply, initializing stuff!");
//						ocamljavaMain.setConstant(Pervasives.class,
//								Pervasives.createConstants());
//						ocamljavaMain.setConstant(Array.class,
//								ocaml.stdlib.Array.createConstants());
//						ocamljavaMain.setConstant(List.class,
//								ocaml.stdlib.List.createConstants());
//						ocamljavaMain.setConstant(Char.class,
//								Char.createConstants());
//						ocamljavaMain.setConstant(ocaml.stdlib.String.class,
//								ocaml.stdlib.String.createConstants());
//						ocamljavaMain.setConstant(Sys.class,
//								Sys.createConstants());
//						ocamljavaMain.setConstant(Marshal.class,
//								Marshal.createConstants());
//						ocamljavaMain.setConstant(Obj.class,
//								Obj.createConstants());
//						ocamljavaMain.setConstant(Int32.class,
//								Int32.createConstants());
//						ocamljavaMain.setConstant(Int64.class,
//								Int64.createConstants());
//						ocamljavaMain.setConstant(Nativeint.class,
//								Nativeint.createConstants());
//						ocamljavaMain.setConstant(Lexing.class,
//								Lexing.createConstants());
//						ocamljavaMain.setConstant(Parsing.class,
//								Parsing.createConstants());
//						ocamljavaMain.setConstant(Set.class,
//								Set.createConstants());
//						ocamljavaMain.setConstant(ocaml.stdlib.Map.class,
//								ocaml.stdlib.Map.createConstants());
//						ocamljavaMain.setConstant(CamlinternalLazy.class,
//								CamlinternalLazy.createConstants());
//						ocamljavaMain.setConstant(Buffer.class,
//								Buffer.createConstants());
//						ocamljavaMain.setConstant(Printf.class,
//								Printf.createConstants());
//						ocamljavaMain.setConstant(Arg.class,
//								Arg.createConstants());
//						ocamljavaMain.setConstant(Digest.class,
//								Digest.createConstants());
//						ocamljavaMain.setConstant(Random.class,
//								Random.createConstants());
//						ocamljavaMain.setConstant(Hashtbl.class,
//								Hashtbl.createConstants());
//						ocamljavaMain.setConstant(Format.class,
//								Format.createConstants());
//						ocamljavaMain.setConstant(Filename.class,
//								Filename.createConstants());
//						ocamljavaMain.setConstant(Misc.class,
//								Misc.createConstants());
//						ocamljavaMain.setConstant(Config.class,
//								Config.createConstants());
//						ocamljavaMain.setConstant(Clflags.class,
//								Clflags.createConstants());
//						ocamljavaMain.setConstant(Terminfo.class,
//								Terminfo.createConstants());
//						ocamljavaMain.setConstant(Warnings.class,
//								Warnings.createConstants());
//						ocamljavaMain.setConstant(Location.class,
//								Location.createConstants());
//						ocamljavaMain.setConstant(Longident.class,
//								Longident.createConstants());
//						ocamljavaMain.setConstant(Syntaxerr.class,
//								Syntaxerr.createConstants());
//						ocamljavaMain.setConstant(Parser.class,
//								Parser.createConstants());
//						ocamljavaMain.setConstant(Lexer.class,
//								Lexer.createConstants());
//						ocamljavaMain.setConstant(Parse.class,
//								Parse.createConstants());
//						ocamljavaMain.setConstant(Ccomp.class,
//								Ccomp.createConstants());
//						ocamljavaMain.setConstant(Pparse.class,
//								Pparse.createConstants());
//						ocamljavaMain.setConstant(Compenv.class,
//								Compenv.createConstants());
//						ocamljavaMain.setConstant(Depend.class,
//								Depend.createConstants());
//						ocamljavaMain.setConstant(Ocamldep.class,
//								Ocamldep.createConstants());
//						getLog().info("inside apply, done initializing stuff!");
//						
//						return ocamljavaMain;
//					}
//				});

	}

	private File rawDependencyTargetFullPath() {
		return new File(chooseDependencyGraphTargetFullPath().getPath()
				+ ".raw");
	}

};
