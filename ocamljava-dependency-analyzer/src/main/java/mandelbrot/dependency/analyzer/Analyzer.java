package mandelbrot.dependency.analyzer;

import java.io.File;

import org.codehaus.plexus.util.StringUtils;

import com.google.common.base.Optional;

/**  
* Given we know module X requires modules Y,Z, etc.,
* this class finds an ordering of the modules such that X < Y iff Y requires X.
* We can also assume that if D = {Y_0, Y_1, ..., Y_N} and X < D, Y < D,
* then X < Y iff string(X) < string(Y)
* X < X is false and of course, X = X.
*/
public class Analyzer {

	public static Optional<String> moduleNameOfSource(final String source) {
		if (StringUtils.isBlank(source))
			return Optional.absent();

		final String name = new File(source).getName();
		final String extension = org.codehaus.plexus.util.FileUtils
				.getExtension(name);

		final String rawModuleName = name.substring(0, name.length()
				- (extension.length() == 0 ? 0 : extension.length()
						+ File.separator.length()));

		final String lowerCasedName = rawModuleName.toLowerCase();

		if (lowerCasedName.isEmpty())
			return Optional.absent();

		return Optional.of(lowerCasedName);

	}
}