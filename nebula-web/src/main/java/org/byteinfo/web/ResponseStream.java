package org.byteinfo.web;

import java.io.IOException;
import java.io.OutputStream;

public class ResponseStream extends ChunkedOutputStream {
	protected HttpContext ctx;
	protected boolean initialized;

	public ResponseStream(OutputStream out, HttpContext ctx) {
		super(out);
		this.ctx = ctx;
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		ensureInitialized();
		super.write(b, off, len);
	}

	@Override
	public void close() throws IOException {
		if (closed) {
			return;
		}
		ensureInitialized();
		super.close();
		ctx.commit(null);
	}

	private void ensureInitialized() throws IOException {
		if (initialized) {
			return;
		}
		initialized = true;
		ctx.sendHeaders();
	}
}
