package org.byteinfo.web;

import java.io.IOException;
import java.io.InputStream;

/**
 * HTTP Result
 */
public interface Result {

	/**
	 * Get the InputStream for accessing the result.
	 *
	 * @return input stream
	 * @throws IOException if an error occurs
	 */
	InputStream stream() throws IOException;

	/**
	 * Get the MediaType of the result.
	 *
	 * @return media type
	 */
	MediaType type();

	/**
	 * Get the result size in bytes.
	 *
	 * @return result size or -1 if undefined
	 */
	long length();

}
