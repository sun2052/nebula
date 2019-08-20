package org.byteinfo.util.net;

/**
 * IPv4
 */
public class IPv4 {

	private int address;

	private IPv4(int address) {
		this.address = address;
	}

	public static IPv4 of(byte[] address) {
		return new IPv4(IPv4Util.toInt(address));
	}

	public static IPv4 of(int address) {
		return new IPv4(IPv4Util.toInt(address));
	}

	public static IPv4 of(long address) {
		return new IPv4(IPv4Util.toInt(address));
	}

	public static IPv4 of(String address) {
		return new IPv4(IPv4Util.toInt(address));
	}

	public byte[] getBytes() {
		return IPv4Util.toBytes(address);
	}

	public int getInt() {
		return address;
	}

	public long getLong() {
		return IPv4Util.toLong(address);
	}

	public String getString() {
		return IPv4Util.toString(address);
	}

	@Override
	public int hashCode() {
		return address;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IPv4 other = (IPv4) obj;
		return address == other.address;
	}

	@Override
	public String toString() {
		return "IPv4 [address=" + getString() + "]";
	}
}
