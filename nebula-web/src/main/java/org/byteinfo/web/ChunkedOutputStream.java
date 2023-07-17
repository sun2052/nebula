package org.byteinfo.web;

import java.io.IOException;
import java.io.OutputStream;

// https://www.rfc-editor.org/rfc/rfc9112#name-transfer-codings
public class ChunkedOutputStream extends OutputStream {
	protected OutputStream out;
	private boolean closed;

	public ChunkedOutputStream(OutputStream out) {
		this.out = out;
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] {(byte) b}, 0, 1);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if (closed) {
			throw new IOException("stream closed");
		}
		writeChunk(len);
		out.write(b, off, len);
		out.write(HttpCodec.CRLF);
	}

	@Override
	public void flush() throws IOException {
		out.flush();
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		closed = true;
		writeChunk(0); // last-chunk
		out.write(HttpCodec.CRLF);
		out.flush();
	}

	private void writeChunk(long size) throws IOException {
		out.write(Long.toHexString(size).getBytes());
		out.write(HttpCodec.CRLF);
	}
}
