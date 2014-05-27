package mandelbrot.ocamljava_maven_plugin;

import static mandelbrot.ocamljava_maven_plugin.OcamlJavaConstants.DOT;
import static mandelbrot.ocamljava_maven_plugin.OcamlJavaConstants.OCAML_SOURCE_FILE_EXTENSIONS;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import mandelbrot.ocamljava_maven_plugin.util.ClassPathGatherer;
import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import ocaml.compilers.ocamljavaMain;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * <p>
 * This is a goal which compiles OCaml sources during the maven compilation
 * phase. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamljava -classpath classpath/lib.jar -c foo.ml bar.ml ...</code>
 * </p>
 * from the command line but instead uses maven properties to infer the source
 * locations and class path. All parameters can be overriden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @requiresProject
 * @goal compile
 * @phase compile
 * @executionStrategy once-per-session
 * @requiresDependencyResolution runtime
 * @threadSafe *
 * @since 1.0
 */
public class OcamlJavaCompileMojo extends OcamlJavaCompileAbstractMojo {
	
	@Override
	protected File chooseOcamlSourcesDirectory() {
		return ocamlSourceDirectory;
	}

	@Override
	protected String chooseOcamlCompiledSourcesTarget() {
		return ocamlCompiledSourcesTarget;
	}

}
