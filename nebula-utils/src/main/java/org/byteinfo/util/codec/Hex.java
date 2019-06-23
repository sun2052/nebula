package org.byteinfo.util.codec;

/**
 * Hex Codec
 */
public interface Hex {
	char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

	static String encode(byte[] data) {
		char[] chars = new char[data.length * 2];
		int i = 0;
		for (byte octet : data) {
			chars[i++] = DIGITS[octet >>> 4 & 0x0f];
			chars[i++] = DIGITS[octet & 0x0f];
		}
		return new String(chars);
	}

	static byte[] decode(String hex) {
		char[] chars = hex.toCharArray();
		if ((chars.length & 0x01) != 0) {
			throw new IllegalArgumentException("Odd number of characters.");
		}

		byte[] bytes = new byte[chars.length / 2];
		for (int i = 0, j = 0; i < bytes.length; i++) {
			int value = toDigit(chars[j++]) << 4;
			value |= toDigit(chars[j++]);
			bytes[i] = (byte) value;
		}

		return bytes;
	}

	private static int toDigit(char ch) {
		int digit = Character.digit(ch, 16);
		if (digit == -1) {
			throw new IllegalArgumentException("Illegal hexadecimal character: " + ch);
		}
		return digit;
	}
}
