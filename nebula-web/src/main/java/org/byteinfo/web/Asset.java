package org.byteinfo.web;

import org.byteinfo.util.codec.ByteUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Base64;

public class Asset implements Result {
	private byte[] data;
	private MediaType type;
	private long length;
	private long modified;
	private String eTag;

	public Asset(byte[] data, MediaType type, long length, long modified) {
		this.data = data;
		this.type = type;
		this.length = length;
		this.modified = modified;
	}

	@Override
	public InputStream stream() throws IOException {
		return new ByteArrayInputStream(data);
	}

	/**
	 * Get the MediaType of the asset.
	 *
	 * @return media type
	 */
	public MediaType type() {
		return type;
	}

	/**
	 * Get the asset size in bytes.
	 *
	 * @return asset size or -1 if undefined
	 */
	public long length() {
		return length;
	}

	/**
	 * Get the last modified time of the asset.
	 *
	 * @return last modified millis or -1 if undefined
	 */
	public long modified() {
		return modified;
	}

	/**
	 * Get a ETag of the asset.
	 *
	 * @return ETag
	 */
	public String eTag() {
		if (eTag == null) {
			ByteBuffer buffer = ByteBuffer.allocate(16);
			buffer.put(ByteUtil.asBytes(length()));
			buffer.put(ByteUtil.asBytes(modified()));
			eTag = "\"" + Base64.getUrlEncoder().encodeToString(buffer.array()) + "\"";
		}
		return eTag;
	}
}