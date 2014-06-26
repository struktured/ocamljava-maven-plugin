package mandelbrot.ocamljava_maven_plugin.util;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Preconditions;

/***
 * Encapsulates typical artifact patterns of the form <code>groupId:artifactId:version:[type]:[classifier]</code>,
 * which is as user would describe it in a pom configuration file.
 *
 */
public class ArtifactDescriptor {

	public static final String ARTIFACT_DESCRIPTOR_SEPARATOR = ":";

	/**
	 * Converts the given artifact to a string of the form <code>groupId:artifactId:version:[type]:[classifier]</code>.
	 * @param artifact the artifact to encode as an artifact descriptor.
	 * @return the artifact descriptor, as a string.
	 */
	public static String toDescriptor(final Artifact artifact) {
		Preconditions.checkNotNull(artifact);
		final String artifactId = artifact.getArtifactId();
		final String groupId = artifact.getGroupId();
		final String type = artifact.getType();
		final String version = artifact.getVersion();
		final String classifier = artifact.getClassifier();
		final StringBuilder builder = new StringBuilder(ArtifactUtils.key(
				groupId, artifactId, version));

		if (!StringUtils.isBlank(type)) {
			builder.append(ARTIFACT_DESCRIPTOR_SEPARATOR + type);
			if (!StringUtils.isBlank(classifier)) {
				builder.append(ARTIFACT_DESCRIPTOR_SEPARATOR + classifier);
			}
		}
		final String artifactDescription = builder.toString();
		return artifactDescription;
	}


	/**
	 * Converts the given Maven project to a string of the form <code>groupId:artifactId</code>.
	 * @param mavenProject the Maven project to encode.
	 * @return the artifact descriptor, as a string.
	 */
	public static String toGoal(final MavenProject mavenProject, final String goal) {
		Preconditions.checkNotNull(mavenProject);
		
		final String artifactId = mavenProject.getArtifactId();
		final String groupId = mavenProject.getGroupId();
		
		final String artifactDescription = groupId + ARTIFACT_DESCRIPTOR_SEPARATOR + artifactId;
		return artifactDescription;
	}
	
}
