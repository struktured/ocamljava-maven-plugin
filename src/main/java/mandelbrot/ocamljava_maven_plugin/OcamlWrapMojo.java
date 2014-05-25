package mandelbrot.ocamljava_maven_plugin;

import java.util.Collection;
import java.util.Set;

import mandelbrot.ocamljava_maven_plugin.util.JarExtractor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * <p>
 * This is a goal which wraps OCaml compiled source interfaces with code
 * generated java equivalents. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamlwrap lib.cmi</code>
 * </p>
 * from the command line but instead uses maven properties to infer the compiled
 * module interface locations. All parameters can be overridden. See the
 * configuration section of the documentation for more information.</p>
 * 
 * @requiresProject
 * @requiresDependencyResolution
 * @goal wrap
 * @phase generate-sources
 * @threadSafe *
 * @since 1.0
 */
public class OcamlWrapMojo extends OcamlJavaAbstractMojo {

	/***
	 * The artifacts that will be retrieved, scanned for ocaml compiled
	 * interfaces, and then code generated for. Expressed with
	 * groupId:artifactId:version[:type[:classifier]] format.
	 * 
	 * @parameter
	 * @required
	 */
	protected Set<String> targetArtifacts = ImmutableSet.of();

	/***
	 * Prefix for names of generated classes. Default value is blank.
	 * 
	 * @parameter default-value=""
	 */
	protected String classNamePrefix = "";

	/***
	 * Suffix for names of generated classes.
	 * 
	 * @parameter default-value="Wrapper"
	 */
	protected String classNameSuffix = "Wrapper";

	/***
	 * Arguments passed for library initialization. Defaults to empty.
	 * 
	 * @parameter
	 */
	protected String[] libraryArgs = new String[] {};

	public static enum LibraryInitMode {
		STATIC, EXPLICIT;

		public String toCommandLineValue() {
			return name().toLowerCase();
		}
	}

	/**
	 * Library initialization mode. One of <code>EXPLICIT</code> or
	 * <code>STATIC</code>.
	 * 
	 * @parameter default-value="EXPLICIT"
	 */
	protected LibraryInitMode libraryInitMode = LibraryInitMode.EXPLICIT;

	/***
	 * Library package.
	 * 
	 * @parameter default-value=""
	 */
	protected String libaryPackage = "";

	/***
	 * Whether to disable warnings.
	 * 
	 * @parameter default-value="false"
	 */
	protected boolean noWarnings;

	// -package <string> "" package of generated classes

	public static enum StringMapping {
		JAVA_STRING, OCAMLSTRING, BYTE_ARRAY;
		public String toCommmandLineArgument() {
			return name().toLowerCase().replace("_", "-");
		}
	}

	/***
	 * Determines the string mapping for OCaml string type. One of
	 * <code>JAVA_STRING</code>, <code>OCAMLSTRING</code>, or
	 * <code>BYTE-ARRAY</code>.
	 * 
	 * @parameter default-value="JAVA_STRING"
	 */
	protected StringMapping stringMapping = StringMapping.JAVA_STRING;

	/***
	 * Whether to enable verbose mode.
	 * 
	 * @parameter default-value="false"
	 * 
	 **/
	protected boolean verbose;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (targetArtifacts == null || targetArtifacts.isEmpty()) {
			getLog().info("no artifacts to wrap");
			return;
		}

		final ImmutableList<String> artifactFiles = getArtifactFiles();

		final ImmutableList.Builder<String> cmiFilesBuilder = ImmutableList
				.builder();

		for (final String artifactFile : artifactFiles) {
			final Collection<String> compiledIntefaces = Collections2.filter(
					new JarExtractor(this).addFiles(artifactFile,
							getOcamlCompiledSourcesTargetFullPath()),
					new Predicate<String>() {
						@Override
						public boolean apply(final String file) {
							return OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION
									.equalsIgnoreCase(FileUtils
											.getExtension(file));
						}
					});
			cmiFilesBuilder.addAll(compiledIntefaces);
		}

		final Optional<String[]> commandLineArguments = generateCommandLineArguments(cmiFilesBuilder
				.build());

		if (commandLineArguments.isPresent())
			org.ocamljava.wrapper.ocamljavaMain
					.main(commandLineArguments.get());
		else
			getLog().info(
					"no compiled module interfaces to wrap in "
							+ getOcamlCompiledSourcesTargetFullPath());
	}

	private ImmutableList<String> getArtifactFiles() {
		final ImmutableList.Builder<String> builder = ImmutableList.builder();

		Collection<Artifact> filter = Collections2.filter(
				project.getArtifacts(), new Predicate<Artifact>() {
					@Override
					public boolean apply(final Artifact artifact) {

						final String artifactId = artifact.getArtifactId();
						final String groupId = artifact.getGroupId();
						final String type = artifact.getType();
						final String version = artifact.getVersion();
						final String classifier = artifact.getClassifier();
						final StringBuilder builder = new StringBuilder(
								ArtifactUtils.key(groupId, artifactId, version));

						if (!StringUtils.isBlank(type)) {
							builder.append(":" + type);
							if (!StringUtils.isBlank(classifier)) {
								builder.append(":" + classifier);
							}
						}
						getLog().info("artifact key: " + builder.toString());
						for (final String targetArtifact : targetArtifacts)
							if (builder.toString().startsWith(targetArtifact))
								return true;

						return false;
					}
				});

		for (final Artifact artifact : filter) {
			getLog().info("adding artifact: " + artifact);
			builder.add(artifact.getFile().getPath());
		}

		final ImmutableList<String> artifactFiles = builder.build();
		return artifactFiles;
	}

	private Optional<String[]> generateCommandLineArguments(
			final Collection<String> files) {
		if (files == null || files.isEmpty())
			return Optional.absent();
		return Optional.of(files.toArray(new String[] {}));
	}

}
