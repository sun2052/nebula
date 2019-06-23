package org.byteinfo.util.codec;

import java.security.SecureRandom;

/**
 * Random Token Generation Utility
 */
public interface RandomUtil {
	SecureRandom RANDOM = new SecureRandom();

	static String getNumeric(int count) {
		return get(count, "0123456789");
	}

	static String getAlphabetic(int count) {
		return get(count, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	static String getAlphaNumeric(int count) {
		return get(count, "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
	}

	static String get(int count, String chars) {
		int length = chars.length();
		char[] result = new char[count];
		for (int i = 0; i < count; i++) {
			result[i] = chars.charAt(RANDOM.nextInt(length));
		}
		return new String(result);
	}

	static String get(int count, char... chars) {
		char[] result = new char[count];
		for (int i = 0; i < count; i++) {
			result[i] = chars[RANDOM.nextInt(chars.length)];
		}
		return new String(result);
	}

	static byte[] get(int byteSize) {
		byte[] bytes = new byte[byteSize];
		RANDOM.nextBytes(bytes);
		return bytes;
	}
}
