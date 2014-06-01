package mandelbrot.ocamljava_maven_plugin;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import mandelbrot.ocamljava_maven_plugin.util.FileMappings;
import mandelbrot.ocamljava_maven_plugin.util.JarExtractor;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

/**
 * <p>
 * This is a goal which wraps OCaml compiled source interfaces with code
 * generated java equivalents. It is the same as executing something like
 * </p>
 * <p>
 * <code>ocamlwrap -package com.mycomp lib.cmi</code>
 * </p>
 * from the command line but instead uses maven parameters to infer the compiled
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

	private static final String ARTIFACT_DESCRIPTOR_SEPARATOR = ":";

	/***
	 * <p>The artifacts to scan for ocaml compiled interfaces and then perform code generation on. 
	 * Expressed with groupId:artifactId:version[:type[:classifier]] format.</p>
	 * 
	 * <p>NOTE: The artifacts must have been packaged as either a jar or zip file.</p>
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
	 * @parameter default-value=""
	 */
	protected String libraryArgs = "";

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
	 * Whether to disable warnings during the code generation process.
	 * 
	 * @parameter default-value="false"
	 */
	protected boolean noWarnings;

	public static enum StringMapping {
		JAVA_STRING, OCAMLSTRING, BYTE_ARRAY;
		public String toCommandLineValue() {
			return name().toLowerCase().replace("_", "-");
		}
	}

	/***
	 * Determines the string mapping for the OCaml string type. One of
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

	/***
	 * Sets the java package name for each source file.
	 * 
	 * @parameter default-value=""
	 * 
	 **/	
	protected String packageName;

	
	/***
	 * Sets how java packages are determined for the code generated classes.
	 * <p>By default, a java package will be inferred according to the folder structure of the modules.
	 * For instance, <code>"src/main/ocaml/foo/bar/lib.ml"</code> will generate <code>package foo.bar</code> at the top of <code>LibWrapper.java</code>.
	 * To fix the package name for all compiled module interfaces, set this value to <code>FIXED</code> and fill in the {@link #packageName} parameter
	 * accordingly.</p>
	 *
	 * @parameter default-value="DYNAMIC"
	 * 
	 **/	
	protected JavaPackageMode javaPackageMode = JavaPackageMode.DYNAMIC;

	
	public static enum JavaPackageMode {
		FIXED,
		DYNAMIC
	}
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if (targetArtifacts == null || targetArtifacts.isEmpty()) {
			getLog().info("no artifacts to wrap");
			return;
		}

		final List<String> artifactFiles = getArtifactFiles();

		final List<String> cmiFiles = extractCompiledModuleInterfaces(artifactFiles);
		
		if (cmiFiles.isEmpty()) {
			getLog().info("no compiled module interfaces found in " + getTargetJarFullPath());
			return;
		}
		
		if (JavaPackageMode.DYNAMIC.equals(javaPackageMode)) {
			getLog().info("infer package names based on directory structure");
			final Multimap<String, String> filesByPackageName = FileMappings.buildPackageMap(cmiFiles);
		
			final Set<String> packageNames = filesByPackageName.keySet();
		
			for (final String packageName : packageNames) {
				final Collection<String> filesInPackage = filesByPackageName.get(packageName);
				wrapFiles(filesInPackage, packageName);
			}
		} else {
			getLog().info("package name is fixed to \"" + packageName + "\"");
			wrapFiles(cmiFiles, packageName);
		}
	}

	private void wrapFiles(final Collection<String> cmiFiles, final String packageName) {
		
		final Collection<String> pathMappings = ImmutableList.of();
		final Optional<String[]> commandLineArguments = generateCommandLineArguments(pathMappings, cmiFiles, packageName);

		if (commandLineArguments.isPresent()) {
			getLog().info("command line arguments: " + ImmutableList.copyOf(commandLineArguments.get()));
			org.ocamljava.wrapper.ocamljavaMain
					.main(commandLineArguments.get());
		}
		else
			getLog().info(
					"no compiled module interfaces to wrap in "
							+ getOcamlCompiledSourcesTargetFullPath());
	}

	private List<String> extractCompiledModuleInterfaces(
			final Collection<String> artifactFiles) {
		
		final ImmutableList.Builder<String> cmiFilesBuilder = ImmutableList.builder();

		for (final String artifactFile : artifactFiles) {
			final Collection<String> compiledIntefaces = 
					new JarExtractor(this).extractFiles(artifactFile,
							getOcamlCompiledSourcesTargetFullPath(), 
							ImmutableSet.of(OcamlJavaConstants.COMPILED_INTERFACE_EXTENSION));
					
			cmiFilesBuilder.addAll(compiledIntefaces);
		}

		final List<String> cmiFiles = cmiFilesBuilder.build();
		return cmiFiles;
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
							builder.append(ARTIFACT_DESCRIPTOR_SEPARATOR + type);
							if (!StringUtils.isBlank(classifier)) {
								builder.append(ARTIFACT_DESCRIPTOR_SEPARATOR + classifier);
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

	private Optional<String[]> generateCommandLineArguments(final Collection<String> includePaths,			
			final Collection<String> files, final String packageName) {
		if (files == null || files.isEmpty())
			return Optional.absent();
		
		final ImmutableList.Builder<String> builder = ImmutableList.builder();
	
		if (noWarnings)
			builder.add(OcamlJavaConstants.NO_WARNINGS_OPTION);
		
		if (verbose)
			builder.add(OcamlJavaConstants.VERBOSE_OPTION);
	

		for (final String includePath : includePaths) {
			if (!StringUtils.isBlank(includePath)) {
				builder.add(OcamlJavaConstants.INCLUDE_DIR_OPTION).add(
						includePath);
			}

		}
		
		if (!StringUtils.isBlank(classNamePrefix)) {
			builder.add(OcamlJavaConstants.CLASS_NAME_PREFIX_OPTION);
			builder.add(classNamePrefix);
		}
		
		if (!StringUtils.isBlank(classNameSuffix)) {
			builder.add(OcamlJavaConstants.CLASS_NAME_SUFFIX_OPTION);
			builder.add(classNameSuffix);
		}
	
		if (libraryInitMode != null) {
			builder.add(OcamlJavaConstants.LIBRARY_INIT_OPTION);
			builder.add(libraryInitMode.toCommandLineValue());
		}

		if (!StringUtils.isBlank(libraryArgs)) {
			builder.add(OcamlJavaConstants.LIBRARY_ARGS_OPTION);
			builder.add(String.format("\"%s\"", libraryArgs));
		}
	
		if (!StringUtils.isBlank(libaryPackage)) {
			builder.add(OcamlJavaConstants.LIBRARY_PACKAGE_OPTION);
			builder.add(libaryPackage);
		}
	
		if (!StringUtils.isBlank(packageName)) {
			builder.add(OcamlJavaConstants.PACKAGE_OPTION);
			builder.add(packageName);
		}
		
		if (stringMapping != null) {
			builder.add(OcamlJavaConstants.STRING_MAPPING_OPTION);
			builder.add(stringMapping.toCommandLineValue());
		}

		builder.addAll(files);
		
		return Optional.of(builder.build().toArray(new String[] {}));
	}

}
