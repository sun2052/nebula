package org.byteinfo.util.io;

import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends InputStream {
	protected InputStream in;
	protected long limit;

	public LimitedInputStream(InputStream in, long limit) {
		this.in = in;
		this.limit = limit;
	}

	@Override
	public int read() throws IOException {
		if (limit == 0) {
			return -1;
		}
		int result = in.read();
		if (result != -1) {
			limit--;
		}
		if (result == -1 && limit > 0) {
			throw new IOException("unexpected end of stream");
		}
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (limit == 0) {
			return -1;
		}
		len = (int) Math.min(len, limit);
		int result = in.read(b, off, len);
		if (result != -1) {
			limit -= result;
		}
		if (result == -1 && limit > 0) {
			throw new IOException("unexpected end of stream");
		}
		return result;
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = in.skip(Math.min(n, limit));
		limit -= skipped;
		return skipped;
	}

	@Override
	public int available() throws IOException {
		return (int) Math.min(in.available(), limit);
	}

	@Override
	public void close() throws IOException {
		limit = 0;
	}
}
