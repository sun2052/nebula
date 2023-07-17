package org.byteinfo.web;

import org.byteinfo.util.io.LimitedInputStream;

import java.io.IOException;
import java.io.InputStream;

// https://www.rfc-editor.org/rfc/rfc9112#name-transfer-codings
public class ChunkedInputStream extends LimitedInputStream {
	public ChunkedInputStream(InputStream in) {
		super(in, 0);
	}

	@Override
	public int read() throws IOException {
		if (limit <= 0 && readChunk() == -1) {
			return -1;
		}
		return super.read();
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (limit <= 0 && readChunk() == -1) {
			return -1;
		}
		return super.read(b, off, len);
	}

	@Override
	public int available() throws IOException {
		if (limit <= 0 && readChunk() == -1) {
			return 0;
		}
		return super.available();
	}

	private long readChunk() throws IOException {
		if (limit == 0) { // read next chunk
			String line = HttpCodec.readLine(in, true);
			String chunkSize = line.split(";", 2)[0].trim(); // ignore chunk-ext and possible BWSs
			limit = Long.parseLong(chunkSize, 16);
			if (limit == 0) { // last-chunk
				limit = -1;
				while (!HttpCodec.readLine(in, false).isEmpty()) {
					// discard possible trailer-section and ending CRLF
				}
			}
		}
		return limit;
	}
}
