package org.byteinfo.web;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.function.Supplier;

public class Result {
	private int status = StatusCode.OK;
	private String type = ContentType.HTML;
	private long length;
	private String eTag;
	private Supplier<InputStream> supplier = InputStream::nullInputStream;

	protected Result() {
	}

	public static Result of(String data) {
		return of(data.getBytes());
	}

	public static Result of(byte[] data) {
		return new Result()
				.setSupplier(() -> new ByteArrayInputStream(data))
				.setLength(data.length);
	}

	public static Result of(URL data) throws IOException {
		URLConnection connection = data.openConnection();
		long length = connection.getContentLengthLong();
		long modified = connection.getLastModified();
		return new Result()
				.setSupplier(() -> {
					try {
						return new BufferedInputStream(data.openStream());
					} catch (IOException e) {
						throw new WebException(e);
					}
				})
				.setLength(length)
				.setETag('"' + Base64.getEncoder().withoutPadding().encodeToString((modified + "-" + length).getBytes()) + '"');
	}


	/* ---------------- Setter -------------- */

	public Result setStatus(int status) {
		this.status = status;
		return this;
	}

	public Result setType(String type) {
		this.type = type;
		return this;
	}

	public Result setLength(long length) {
		this.length = length;
		return this;
	}

	public Result setETag(String eTag) {
		this.eTag = eTag;
		return this;
	}

	public Result setSupplier(Supplier<InputStream> supplier) {
		this.supplier = supplier;
		return this;
	}


	/* ---------------- Getter -------------- */

	public int status() {
		return status;
	}

	public String type() {
		return type;
	}

	public long length() {
		return length;
	}

	public String eTag() {
		return eTag;
	}

	public InputStream stream() {
		return supplier.get();
	}
}
