package org.byteinfo.util.net;

import java.net.InetAddress;

public interface IPv4Util {
	static boolean isPrivate(InetAddress address) {
		return isPrivate(address.getAddress());
	}

	static boolean isPrivate(String address) {
		return isPrivate(toBytes(address));
	}

	static boolean isPrivate(byte[] address) {
		if (address.length != 4) {
			return false;
		}
		int octet1 = Byte.toUnsignedInt(address[0]);
		int octet2 = Byte.toUnsignedInt(address[1]);
		return switch (octet1) {
			case 10, 127 -> true;
			case 172 -> octet2 >= 16 && octet2 <= 31;
			case 192 -> octet2 == 168;
			case 169 -> octet2 == 254;
			default -> false;
		};
	}

	static boolean isPrivate(int address) {
		return isPrivate(toBytes(address));
	}

	static byte[] toBytes(InetAddress address) {
		return address.getAddress();
	}

	static byte[] toBytes(String address) {
		String[] octets = address.split("\\.");
		return new byte[] {(byte) Integer.parseInt(octets[0]), (byte) Integer.parseInt(octets[1]), (byte) Integer.parseInt(octets[2]), (byte) Integer.parseInt(octets[3])};
	}

	static byte[] toBytes(int address) {
		return new byte[] {(byte) (address >>> 24), (byte) (address >>> 16 & 0xff), (byte) (address >>> 8 & 0xff), (byte) (address & 0xff)};
	}

	static int toInt(InetAddress address) {
		return toInt(toBytes(address));
	}

	static int toInt(String address) {
		return toInt(toBytes(address));
	}

	static int toInt(byte[] address) {
		int result = 0;
		for (byte octet : address) {
			result <<= 8;
			result |= octet & 0xff;
		}
		return result;
	}

	static String toString(InetAddress address) {
		return toString(address.getAddress());
	}

	static String toString(byte[] address) {
		return (address[0] & 0xff) + "." + (address[1] & 0xff) + "." + (address[2] & 0xff) + "." + (address[3] & 0xff);
	}

	static String toString(int address) {
		return (address >>> 24) + "." + (address >>> 16 & 0xff) + "." + (address >>> 8 & 0xff) + "." + (address & 0xff);
	}
}
