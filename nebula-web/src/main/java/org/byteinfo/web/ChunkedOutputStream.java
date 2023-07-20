package org.byteinfo.web;

import java.io.IOException;
import java.io.OutputStream;

// https://www.rfc-editor.org/rfc/rfc9112#name-transfer-codings
public class ChunkedOutputStream extends OutputStream {
	protected final byte[] CRLF = {'\r', '\n'};
	protected OutputStream out;
	protected boolean closed;

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
		out.write(CRLF);
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
		out.flush();
	}

	private void writeChunk(long size) throws IOException {
		String header = Long.toHexString(size) + "\r\n";
		if (size == 0) {
			header += "\r\n";
		}
		out.write(header.getBytes());
	}
}
