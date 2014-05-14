package mandelbrot.ocamljava_maven_plugin.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;

public class MavenClassLoader
{

	private final MavenProject project;
	private final AbstractMojo mojo;
	
	public MavenClassLoader(final AbstractMojo mojo, MavenProject project)
	{
		this.mojo = mojo;
		this.project = project;
	}
	
	
	/***
	 * Gets a class loader using a standard URL class loader combined with maven project runtime class paths.
	 * @return a url class loader.
	 * @throws MalformedURLException
	 * @throws DependencyResolutionRequiredException
	 * @throws ClassNotFoundException
	 */
	public ClassLoader loadURLClassLoader() throws MalformedURLException, DependencyResolutionRequiredException, ClassNotFoundException 
	{		
		final URL[] runtimeUrls = getRuntimeUrls();
		final URLClassLoader newLoader = new URLClassLoader(runtimeUrls,
		  Thread.currentThread().getContextClassLoader());
		return newLoader;
	}

	protected URL[] getRuntimeUrls()
			throws DependencyResolutionRequiredException, MalformedURLException
	{
		final List<String> runtimeClasspathElements = getRuntimeUrlsList(project);
		final URL[] runtimeUrls = new URL[runtimeClasspathElements.size()];
		for (int i = 0; i < runtimeClasspathElements.size(); i++) {
		  final String element = runtimeClasspathElements.get(i);
		  runtimeUrls[i] = new File(element).toURI().toURL();
		  mojo.getLog().info("element: " + element);		  
		}
		return runtimeUrls;
	}
	
	protected List<String> getRuntimeUrlsList(final MavenProject project)
			throws DependencyResolutionRequiredException, MalformedURLException
	{
		final List<String> runtimeClasspathElements = project.getRuntimeClasspathElements();
		
		final List<MavenProject> collectedProjects = project.getCollectedProjects();
		
		for (final MavenProject mavenProject : collectedProjects)
		{
			mojo.getLog().info("mavenProject: " + mavenProject.getArtifactId());			
			runtimeClasspathElements.addAll(getRuntimeUrlsList(mavenProject));
		}
		return runtimeClasspathElements;
	}
	 
}
