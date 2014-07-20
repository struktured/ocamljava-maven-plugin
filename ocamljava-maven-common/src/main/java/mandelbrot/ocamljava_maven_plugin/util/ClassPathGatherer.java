package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class ClassPathGatherer
{

	private final AbstractMojo mojo;
	
	public ClassPathGatherer(final AbstractMojo mojo)
	{
		this.mojo = Preconditions.checkNotNull(mojo);
	}

	protected Set<String> getClassPathRec(final MavenProject project, final boolean isTest)
			throws DependencyResolutionRequiredException, MalformedURLException
	{
		
		final ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder()
				.addAll(project.getCompileClasspathElements())
				.addAll(project.getRuntimeClasspathElements())
				.addAll(project.getSystemClasspathElements());
		
		if (isTest)
			builder.addAll(project.getTestClasspathElements());
		
		final List<MavenProject> collectedProjects = project.getCollectedProjects();
		
		for (final MavenProject mavenProject : collectedProjects)
		{
			mojo.getLog().info("mavenProject: " + mavenProject.getArtifactId());			
			builder.addAll(getClassPathRec(mavenProject, isTest));
		}
		return builder.build();
	}

	public List<String> getClassPath(final MavenProject project, final boolean isTest) throws MojoExecutionException {
		try {
			return ImmutableList.<String>builder()
					.addAll(getClassPathRec(project, isTest))
					.build();
		} catch (final MalformedURLException | DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("failed to get runtime url list", e);
		}
	}

	public URL[] getClassPathUrls(final MavenProject project, final boolean isTest) throws MojoExecutionException {
		return Collections2.transform(getClassPath(project, isTest), new Function<String, URL>() {
			@Override
			public URL apply(final String input) {
				try {
					if (mojo.getLog().isDebugEnabled()) {
						mojo.getLog().debug("classpath url: " + input);
					}
					return new File(input).toURI().toURL();
				} catch (final Exception e) {
					throw new RuntimeException("url issue", e);
				}
			}
		}).toArray(new URL[] {});
	}
	 
}
