package org.byteinfo.util.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {
	private long left;

	public LimitedInputStream(InputStream in, long limit) {
		super(in);
		this.left = limit;
	}

	@Override
	public int read() throws IOException {
		if (left == 0) {
			return -1;
		}
		int result = in.read();
		if (result != -1) {
			left--;
		}
		return result;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if (left == 0) {
			return -1;
		}
		len = (int) Math.min(len, left);
		int result = in.read(b, off, len);
		if (result != -1) {
			left -= result;
		}
		return result;
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = in.skip(Math.min(n, left));
		left -= skipped;
		return skipped;
	}

	@Override
	public int available() throws IOException {
		return (int) Math.min(in.available(), left);
	}

	@Override
	public void close() throws IOException {
		left = 0;
	}

	@Override
	public boolean markSupported() {
		return false;
	}
}