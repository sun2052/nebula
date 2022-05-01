package org.byteinfo.util.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * IOUtil
 */
public interface IOUtil {
	/**
	 * Gets an url for accessing the specified resource from classpath.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static URL classResource(String resource) {
		return Thread.currentThread().getContextClassLoader().getResource(resource);
	}

	/**
	 * Gets a url for accessing the specified resource from file system.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static URL fileResource(String resource) {
		Path path = Path.of(resource);
		if (Files.exists(path)) {
			try {
				return path.normalize().toUri().toURL();
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		return null;
	}

	/**
	 * Gets an url for accessing the specified resource from either classpath or file system.
	 *
	 * @param resource resource name, use prefix "class:" for classpath, use prefix "file:" for file system
	 * @return url or null if not found
	 */
	static URL resource(String resource) {
		String[] parts = resource.split(":", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid Resource Syntax: " + resource);
		}
		if ("class".equals(parts[0])) {
			return classResource(parts[1]);
		} else if ("file".equals(parts[0])) {
			return fileResource(parts[1]);
		} else {
			throw new IllegalArgumentException("Invalid Resource Type: " + parts[0]);
		}
	}

	/**
	 * Gets an url for accessing the specified resource from classpath.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static InputStream resourceStream(String resource) {
		InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
		if (stream == null) {
			return null;
		}
		return new BufferedInputStream(stream);
	}

	/**
	 * Gets an url for accessing the specified resource from file system.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static InputStream fileStream(String resource) throws IOException {
		return new BufferedInputStream(Files.newInputStream(Path.of(resource)));
	}

	/**
	 * Gets an input stream for reading the specified url.
	 *
	 * @param url url object
	 * @return input stream or null if not found
	 * @throws IOException if something wrong occurs
	 */
	static InputStream stream(URL url) throws IOException {
		if (url != null) {
			return new BufferedInputStream(url.openStream());
		}
		return null;
	}

	/**
	 * Gets an input stream for reading the specified resource from either classpath or file system.
	 *
	 * @param resource resource name, use prefix "class:" for classpath, use prefix "file:" for file system
	 * @return input stream or null if not found
	 * @throws IOException if something wrong occurs
	 */
	static InputStream stream(String resource) throws IOException {
		return stream(resource(resource));
	}

	/**
	 * Gets a reader for accessing the specified resource from classpath.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static Reader resourceReader(String resource) {
		return reader(resourceStream(resource));
	}

	/**
	 * Gets a reader for accessing the specified resource from file system.
	 *
	 * @param resource resource name
	 * @return url or null if not found
	 */
	static Reader fileReader(String resource) throws IOException {
		return Files.newBufferedReader(Path.of(resource));
	}

	/**
	 * Gets a reader for reading the specified resource from either classpath or file system.
	 *
	 * @param resource resource name, use prefix "class:" for classpath, use prefix "file:" for file system
	 * @return reader or null if not found
	 * @throws IOException if something wrong occurs
	 */
	static Reader reader(String resource) throws IOException {
		return reader(stream(resource));
	}

	/**
	 * Gets a reader for reading the specified url.
	 *
	 * @param url url object
	 * @return reader
	 * @throws IOException if something wrong occurs
	 */
	static Reader reader(URL url) throws IOException {
		if (url == null) {
			return null;
		}
		return reader(url.openStream());
	}

	/**
	 * Gets the reader for the given class path resource.
	 *
	 * @param stream class path resource
	 * @return reader
	 */
	static Reader reader(InputStream stream) {
		if (stream == null) {
			return null;
		}
		return new InputStreamReader(stream);
	}

	/**
	 * Closes the specified closeable resource quietly.
	 *
	 * @param closeable the resource to be closed
	 */
	static void closeQuietly(Closeable closeable) {
		if (closeable == null) {
			return;
		}
		try {
			closeable.close();
		} catch (Exception e) {
			// ignore
		}
	}

	/**
	 * Gets all the bytes from the input stream.
	 *
	 * @param in target input stream
	 * @return byte[] or null if in is null
	 * @throws IOException if something wrong occurs
	 */
	static byte[] toBytes(InputStream in) throws IOException {
		if (in == null) {
			return null;
		}
		try (in) {
			return in.readAllBytes();
		}
	}

	/**
	 * Gets all the bytes from the url.
	 *
	 * @param url target url
	 * @return byte[] or null if url is null
	 * @throws IOException if something wrong occurs
	 */
	static byte[] toBytes(URL url) throws IOException {
		if (url == null) {
			return null;
		}
		return toBytes(url.openStream());
	}

	/**
	 * Gets all the bytes from the file path.
	 *
	 * @param path target file path
	 * @return byte[] or null if path is null
	 * @throws IOException if something wrong occurs
	 */
	static byte[] toBytes(Path path) throws IOException {
		if (path == null) {
			return null;
		}
		return Files.readAllBytes(path);
	}

	/**
	 * Gets all the content from the specified path as a string.
	 *
	 * @param reader target reader
	 * @return string or null if reader is null
	 * @throws IOException if something wrong occurs
	 */
	static String toString(Reader reader) throws IOException {
		if (reader == null) {
			return null;
		}
		try (reader) {
			StringWriter out = new StringWriter(8192);
			reader.transferTo(out);
			return out.toString();
		}
	}

	/**
	 * Gets all the content from the specified path as a string.
	 *
	 * @param in target input stream
	 * @return string or null if in is null
	 * @throws IOException if something wrong occurs
	 */
	static String toString(InputStream in) throws IOException {
		if (in == null) {
			return null;
		}
		return toString(new InputStreamReader(in));
	}

	/**
	 * Gets all the content from the specified path as a string.
	 *
	 * @param url target url
	 * @return string or null if url is null
	 * @throws IOException if something wrong occurs
	 */
	static String toString(URL url) throws IOException {
		if (url == null) {
			return null;
		}
		return toString(url.openStream());
	}

	/**
	 * Gets all the content from the specified path as a string.
	 *
	 * @param path target file path
	 * @return string or null if path is null
	 * @throws IOException if something wrong occurs
	 */
	static String toString(Path path) throws IOException {
		if (path == null) {
			return null;
		}
		return Files.readString(path);
	}

	/**
	 * Gets all the lines from the specified reader.
	 *
	 * @param reader target reader
	 * @return lines list
	 * @throws IOException if something wrong occurs
	 */
	static List<String> toLines(Reader reader) throws IOException {
		if (reader == null) {
			return List.of();
		}
		try (BufferedReader buffer = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader)) {
			return buffer.lines().collect(Collectors.toList());
		}
	}

	/**
	 * Gets all the lines from the specified input stream.
	 *
	 * @param in target input stream
	 * @return lines list
	 * @throws IOException if something wrong occurs
	 */
	static List<String> toLines(InputStream in) throws IOException {
		if (in == null) {
			return List.of();
		}
		return toLines(new InputStreamReader(in));
	}

	/**
	 * Gets all the lines from the specified path.
	 *
	 * @param url target url
	 * @return lines list
	 * @throws IOException if something wrong occurs
	 */
	static List<String> toLines(URL url) throws IOException {
		if (url == null) {
			return List.of();
		}
		return toLines(url.openStream());
	}

	/**
	 * Gets all the lines from the specified path.
	 *
	 * @param path target file path
	 * @return lines list
	 * @throws IOException if something wrong occurs
	 */
	static List<String> toLines(Path path) throws IOException {
		if (path == null) {
			return List.of();
		}
		return Files.readAllLines(path);
	}
}
