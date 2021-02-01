package org.byteinfo.util.io;

import java.io.IOException;
import java.io.Reader;

/**
 * ResourceLoader
 */
@FunctionalInterface
public interface ResourceLoader {
	/**
	 * Returns a reader for the resource with the supplied name. Reader must be closed by callee.
	 *
	 * @param name resource name, e.g. main/main.html
	 * @return reader for the resource
	 * @throws IOException if the resource could not be loaded for any reason.
	 */
	Reader load(String name) throws IOException;

	/**
	 * Empty ResourceLoader
	 */
	ResourceLoader NONE = name -> {
		throw new UnsupportedOperationException("ResourceLoader NOT Configured.");
	};

	/**
	 * Gets a ClassPath ResourceLoader.
	 *
	 * @param root root path, e.g. views/
	 * @return classpath ResourceLoader
	 */
	static ResourceLoader ofClassPath(String root) {
		return name -> IOUtil.resourceReader(root + name);
	}
}
