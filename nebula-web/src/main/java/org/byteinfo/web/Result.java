package org.byteinfo.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Supplier;

public class Result {
	protected String type;
	protected long length;
	protected Supplier<InputStream> supplier;

	public Result() {
	}

	public Result(byte[] data, String type) {
		this.type = type;
		this.length = data.length;
		this.supplier = () -> new ByteArrayInputStream(data);
	}

	public Result(URL url, String type) throws IOException {
		this.type = type;
		this.length = url.openConnection().getContentLengthLong();
		this.supplier = () -> {
			try {
				return new BufferedInputStream(url.openStream());
			} catch (IOException e) {
				throw new WebException(e);
			}
		};
	}

	public InputStream stream() {
		return supplier.get();
	}

	public String type() {
		return type;
	}

	public long length() {
		return length;
	}
}
