package mandelbrot.ocamljava_maven_plugin;

import java.io.File;

public abstract class OcamlBuildAbstractMojo extends OcamlJavaAbstractMojo {

	public String getTargetJarFullPath() {
		return outputDirectory.getPath() + File.separator + chooseTargetJar();
	}
	
	protected abstract String chooseTargetJar();

	public String getTargetOcamlJarFullPath() {
		return outputDirectory.getPath() + File.separator + chooseTargetOcamlJar();
	}

	protected abstract String chooseTargetOcamlJar();

}
