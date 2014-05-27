package mandelbrot.ocamljava_maven_plugin.util;

import java.net.MalformedURLException;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class ClassPathGatherer
{

	private final AbstractMojo mojo;
	
	public ClassPathGatherer(final AbstractMojo mojo)
	{
		this.mojo = Preconditions.checkNotNull(mojo);
	}

	protected List<String> getClassPathRec(final MavenProject project, final boolean isTest)
			throws DependencyResolutionRequiredException, MalformedURLException
	{
		final ImmutableList.Builder<String> builder = ImmutableList.<String>builder().addAll(
				project.getRuntimeClasspathElements());
		
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
			return getClassPathRec(project, isTest);
		} catch (final MalformedURLException | DependencyResolutionRequiredException e) {
			throw new MojoExecutionException("failed to get runtime url list", e);
		}
	}
	 
}
