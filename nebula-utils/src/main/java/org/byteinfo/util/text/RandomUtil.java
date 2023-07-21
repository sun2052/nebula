package org.byteinfo.util.text;

import java.security.SecureRandom;

/**
 * Random Token Generation Utility
 */
public interface RandomUtil {
	SecureRandom RANDOM = new SecureRandom();

	static byte[] random(int byteSize) {
		byte[] bytes = new byte[byteSize];
		RANDOM.nextBytes(bytes);
		return bytes;
	}

	static String random(int count, String chars) {
		int length = chars.length();
		char[] result = new char[count];
		for (int i = 0; i < count; i++) {
			result[i] = chars.charAt(RANDOM.nextInt(length));
		}
		return new String(result);
	}

	static String randomNumeric(int count) {
		return random(count, "0123456789");
	}

	static String randomAlphabetic(int count) {
		return random(count, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	static String randomAlphaNumeric(int count) {
		return random(count, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}
}
