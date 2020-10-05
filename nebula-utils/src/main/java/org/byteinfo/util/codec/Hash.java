package org.byteinfo.util.codec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Hash Utility
 */
public interface Hash {
	String HASH_ALGORITHM = "SHA-512";

	static String hash(String... strings) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
		for (String string : strings) {
			digest.update(string.getBytes(StandardCharsets.UTF_8));
		}
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest.digest());
	}
}
