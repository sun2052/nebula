package org.byteinfo.util.codec;

import java.nio.ByteOrder;

/**
 * ByteUtil
 */
public interface ByteUtil {
	static byte[] asBytes(short value) {
		return asBytes(value, ByteOrder.BIG_ENDIAN);
	}

	static byte[] asBytes(int value) {
		return asBytes(value, ByteOrder.BIG_ENDIAN);
	}

	static byte[] asBytes(long value) {
		return asBytes(value, ByteOrder.BIG_ENDIAN);
	}

	static byte[] asUTF16Bytes(String value) {
		return asUTF16Bytes(value, ByteOrder.BIG_ENDIAN);
	}

	static byte[] asBytes(short value, ByteOrder byteOrder) {
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			return new byte[] {(byte) (value >>> 8), (byte) (value)};
		} else {
			return new byte[] {(byte) (value), (byte) (value >>> 8)};
		}
	}

	static byte[] asBytes(int value, ByteOrder byteOrder) {
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			return new byte[] {(byte) (value >>> 8 * 3), (byte) (value >>> 8 * 2), (byte) (value >>> 8), (byte) value};
		} else {
			return new byte[] {(byte) value, (byte) (value >>> 8), (byte) (value >>> 8 * 2), (byte) (value >>> 8 * 3)};
		}
	}

	static byte[] asBytes(long value, ByteOrder byteOrder) {
		byte[] bytes = new byte[8];
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			for (int i = 0; i < 8; i++) {
				bytes[i] = (byte) (value >>> 8 * (7 - i));
			}
		} else {
			for (int i = 0; i < 8; i++) {
				bytes[i] = (byte) (value >>> 8 * i);
			}
		}
		return bytes;
	}

	static byte[] asUTF16Bytes(String value, ByteOrder byteOrder) {
		char[] chars = value.toCharArray();
		byte[] bytes = new byte[chars.length * 2];
		int i = 0;
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			for (char c : chars) {
				bytes[i++] = (byte) (c >>> 8);
				bytes[i++] = (byte) c;
			}
		} else {
			for (char c : chars) {
				bytes[i++] = (byte) c;
				bytes[i++] = (byte) (c >>> 8);
			}
		}
		return bytes;
	}

	static short asShort(byte[] bytes) {
		return asShort(bytes, ByteOrder.BIG_ENDIAN);
	}

	static int asInt(byte[] bytes) {
		return asInt(bytes, ByteOrder.BIG_ENDIAN);
	}

	static long asLong(byte[] bytes) {
		return asLong(bytes, ByteOrder.BIG_ENDIAN);
	}

	static String asStringByUTF16Bytes(byte[] bytes) {
		return asStringByUTF16Bytes(bytes, ByteOrder.BIG_ENDIAN);
	}

	static short asShort(byte[] bytes, ByteOrder byteOrder) {
		short value = 0;
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			value |= bytes[0] & 0xFF;
			value <<= 8;
			value |= bytes[1] & 0xFF;
		} else {
			value |= bytes[1] & 0xFF;
			value <<= 8;
			value |= bytes[0] & 0xFF;
		}
		return value;
	}

	static int asInt(byte[] bytes, ByteOrder byteOrder) {
		int value = 0;
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			for (byte octet : bytes) {
				value <<= 8;
				value |= octet & 0xFF;
			}
		} else {
			for (int i = 0; i < 4; i++) {
				value <<= 8;
				value |= bytes[3 - i] & 0xFF;
			}
		}
		return value;
	}

	static long asLong(byte[] bytes, ByteOrder byteOrder) {
		long value = 0;
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			for (byte octet : bytes) {
				value <<= 8;
				value |= octet & 0xFF;
			}
		} else {
			for (int i = 0; i < 8; i++) {
				value <<= 8;
				value |= bytes[7 - i] & 0xFF;
			}
		}
		return value;
	}

	static String asStringByUTF16Bytes(byte[] bytes, ByteOrder byteOrder) {
		char[] chars = new char[bytes.length / 2];
		int j = 0;
		if (ByteOrder.BIG_ENDIAN == byteOrder) {
			for (int i = 0; i < bytes.length; i += 2) {
				char c = 0;
				c |= bytes[i] & 0xFF;
				c <<= 8;
				c |= bytes[i + 1] & 0xFF;
				chars[j++] = c;
			}
		} else {
			for (int i = 0; i < bytes.length; i += 2) {
				char c = 0;
				c |= bytes[i + 1] & 0xFF;
				c <<= 8;
				c |= bytes[i] & 0xFF;
				chars[j++] = c;
			}
		}
		return new String(chars);
	}

	static int asUnsignedShort(byte[] bytes) {
		return asUnsignedShort(bytes, ByteOrder.BIG_ENDIAN);
	}

	static long asUnsignedInt(byte[] bytes) {
		return asUnsignedInt(bytes, ByteOrder.BIG_ENDIAN);
	}

	static int asUnsignedShort(byte[] bytes, ByteOrder byteOrder) {
		return asShort(bytes, byteOrder) & 0xFF_FF;
	}

	static long asUnsignedInt(byte[] bytes, ByteOrder byteOrder) {
		return asInt(bytes, byteOrder) & 0xFF_FF_FF_FFL;
	}

	static byte[] asUTF8BytesByCodePoint(int codePoint) {
		int octetCount;
		if (codePoint <= 0b0111_1111) { // 0xxx xxxx
			octetCount = 1;
		} else if (codePoint <= 0b0000_0111___1111_1111) { // 110x xxxx   10xx xxxx
			octetCount = 2;
		} else if (codePoint <= 0b1111_1111___1111_1111) { // 1110 xxxx   10xx xxxx   10xx xxxx
			octetCount = 3;
		} else if (codePoint <= 0b0001_1111___1111_1111___1111_1111) { // 1111 0xxx   10xx xxxx   10xx xxxx   10xx xxxx
			octetCount = 4;
		} else if (codePoint <= 0b0000_0011___1111_1111___1111_1111___1111_1111) { // 1111 10xx   10xx xxxx   10xx xxxx   10xx xxxx   10xx xxxx
			octetCount = 5;
		} else { // 1111 110x   10xx xxxx   10xx xxxx   10xx xxxx   10xx xxxx   10xx xxxx
			octetCount = 6;
		}

		byte[] bytes = new byte[octetCount];
		if (octetCount == 1) {
			bytes[0] = (byte) codePoint;
		} else {
			for (int i = octetCount - 1; i > 0; i--) {
				bytes[i] = (byte) (codePoint & 0b0011_1111 | 0b1000_0000);
				codePoint >>>= 6;
			}
			bytes[0] = (byte) (codePoint | 0b1111_1111 << 8 - octetCount);
		}

		return bytes;
	}

	static int asCodePointByUTF8Bytes(byte[] bytes) {
		int octetCount = 0;
		byte octet = bytes[0];
		while ((octet >>> 7 - octetCount & 1) == 1 && octetCount < 8) {
			octetCount++;
		}

		int codePoint = 0;
		if (octetCount == 0) {
			codePoint |= octet;
		} else {
			int bits = octetCount + 25;
			codePoint |= octet << bits >>> bits;
			for (int i = 1; i < octetCount; i++) {
				codePoint <<= 6;
				codePoint |= bytes[i] & 0b0011_1111;
			}
		}

		return codePoint;
	}
}
