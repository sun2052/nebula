package org.byteinfo.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * IOUtil
 */
public interface IOUtil {
	/**
	 * Gets a url for accessing the specified resource from classpath.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static URL getClassResource(String resource) {
		return Thread.currentThread().getContextClassLoader().getResource(resource);
	}

	/**
	 * Gets a url for accessing the specified resource from file system.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static URL getFileResource(String resource) throws MalformedURLException {
		Path path = Path.of(resource);
		if (Files.exists(path)) {
			return path.normalize().toUri().toURL();
		}
		return null;
	}

	/**
	 * Gets a url for accessing the specified resource from either classpath or file system.
	 *
	 * @param resource resource name, use prefix "class:" for classpath, use prefix "file:" for file system
	 * @return url or null if not found
	 */
	static URL getResource(String resource) throws MalformedURLException {
		String[] parts = resource.split(":", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid Resource Syntax: " + resource);
		}
		if ("class".equals(parts[0])) {
			return getClassResource(parts[1]);
		} else if ("file".equals(parts[0])) {
			return getFileResource(parts[1]);
		} else {
			throw new IllegalArgumentException("Invalid Resource Type: " + parts[0]);
		}
	}

	/**
	 * Gets an input stream for reading the specified resource from either classpath or file system.
	 *
	 * @param resource resource name, use prefix "class:" for classpath, use prefix "file:" for file system
	 * @return input stream or null if not found
	 * @throws IOException if something wrong occurs
	 */
	static InputStream getStream(String resource) throws IOException {
		URL url = getResource(resource);
		if (url != null) {
			return url.openStream();
		}
		return null;
	}

	/**
	 * Gets a reader for reading the specified resource from either classpath or file system.
	 *
	 * @param resource resource name, use prefix "class:" for classpath, use prefix "file:" for file system
	 * @return reader or null if not found
	 * @throws IOException if something wrong occurs
	 */
	static Reader getReader(String resource) throws IOException {
		InputStream stream = getStream(resource);
		if (stream != null) {
			return getReader(getStream(resource));
		}
		return null;
	}

	/**
	 * Gets a reader for reading the specified url.
	 *
	 * @param url url object
	 * @return reader
	 * @throws IOException if something wrong occurs
	 */
	static Reader getReader(URL url) throws IOException {
		return getReader(url.openStream());
	}

	/**
	 * Gets the reader for the given class path resource.
	 *
	 * @param stream class path resource
	 * @return reader
	 */
	static Reader getReader(InputStream stream) {
		return new InputStreamReader(stream, StandardCharsets.UTF_8);
	}

	/**
	 * Closes the specified closeable resource quietly.
	 *
	 * @param closeable the resource to be closed
	 */
	static void closeQuietly(AutoCloseable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception e) {
			// ignore
		}
	}
}
