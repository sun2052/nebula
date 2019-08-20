package org.byteinfo.util.net;

/**
 * IPv4 Utility
 */
public interface IPv4Util {

	static byte[] toBytes(int address) {
		return new byte[] {(byte) (address >>> 24), (byte) (address >>> 16 & 0xff), (byte) (address >>> 8 & 0xff), (byte) (address & 0xff)};
	}

	static byte[] toBytes(long address) {
		return toBytes((int) address);
	}

	static byte[] toBytes(String address) {
		String[] octets = address.split("\\.");
		return new byte[] {(byte) Integer.parseInt(octets[0]), (byte) Integer.parseInt(octets[1]), (byte) Integer.parseInt(octets[2]), (byte) Integer.parseInt(octets[3])};
	}

	static int toInt(byte[] address) {
		return (int) toLong(address);
	}

	static int toInt(long address) {
		return (int) address;
	}

	static int toInt(String address) {
		return (int) toLong(address);
	}

	static long toLong(byte[] address) {
		long result = 0L;
		for (byte octet : address) {
			result <<= 8;
			result |= octet & 0xff;
		}
		return result;
	}

	static long toLong(int address) {
		return address & 0xff_ff_ff_ffL;
	}

	static long toLong(String address) {
		long result = 0L;
		for (String part : address.split("\\.")) {
			result <<= 8;
			result |= Integer.parseInt(part);
		}
		return result;
	}

	static String toString(byte[] address) {
		return (address[0] & 0xff) + "." + (address[1] & 0xff) + "." + (address[2] & 0xff) + "." + (address[3] & 0xff);
	}

	static String toString(int address) {
		return (address >>> 24) + "." + (address >>> 16 & 0xff) + "." + (address >>> 8 & 0xff) + "." + (address & 0xff);
	}

	static String toString(long address) {
		return toString((int) address);
	}

}
