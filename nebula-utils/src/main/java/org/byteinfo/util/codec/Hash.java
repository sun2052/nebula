package org.byteinfo.util.codec;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Hash Utility
 */
public final class Hash {
	public static final Hash MD5 = new Hash("MD5");
	public static final Hash SHA1 = new Hash("SHA-1");
	public static final Hash SHA256 = new Hash("SHA-256");
	public static final Hash SHA512 = new Hash("SHA-512");

	private static final Base64.Encoder BASE64_ENCODER = Base64.getUrlEncoder().withoutPadding();
	private final String algorithm;

	private Hash(String algorithm) {
		this.algorithm = algorithm;
	}

	public byte[] toBytes(byte[]... dataList) {
		try {
			MessageDigest digest = MessageDigest.getInstance(algorithm);
			for (byte[] data : dataList) {
				digest.update(data);
			}
			return digest.digest();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public byte[] toBytes(String... dataList) {
		byte[][] bytes = new byte[dataList.length][];
		for (int i = 0; i < dataList.length; i++) {
			bytes[i] = dataList[i].getBytes();
		}
		return toBytes(bytes);
	}

	public String toHex(byte[]... dataList) {
		return Hex.encode(toBytes(dataList));
	}

	public String toHex(String... dataList) {
		return Hex.encode(toBytes(dataList));
	}

	public String toBase64(byte[]... dataList) {
		return BASE64_ENCODER.encodeToString(toBytes(dataList));
	}

	public String toBase64(String... dataList) {
		return BASE64_ENCODER.encodeToString(toBytes(dataList));
	}
}
