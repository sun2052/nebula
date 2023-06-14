package org.byteinfo.socket;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

/**
 * Message Format
 * {@snippet :
 * +---------+---------+--------------+
 * | Type    | Length  | Data         |
 * | 4 bytes | 8 bytes | Length bytes |
 * +---------+---------+--------------+
 * }
 *
 * @param type type of the message
 * @param length length of the message
 * @param stream input stream for reading the message data
 * @param origin origin of the message
 */
public record Message(int type, long length, InputStream stream, InetSocketAddress origin) {
	public static final int TYPE_SIZE = 4;
	public static final int LENGTH_SIZE = 8;

	/**
	 * Reads all the bytes of the message.
	 *
	 * @return the bytes read
	 * @throws EOFException if end of stream is reached unexpectedly
	 * @throws IOException if an io error occurs
	 */
	public byte[] bytes() throws IOException {
		var bytes = stream.readAllBytes();
		if (bytes.length != length) {
			throw new EOFException();
		}
		return bytes;
	}
}
