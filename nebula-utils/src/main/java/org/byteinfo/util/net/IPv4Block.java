package org.byteinfo.util.net;

import java.math.BigInteger;

/**
 * IPv4 Block
 */
public class IPv4Block {

	private long network;
	private long broadcast;
	private int prefix;
	private long size;

	private long current;

	private IPv4Block(String block) {
		String[] parts = block.split("/");
		prefix = Integer.parseInt(parts[1]);
		int host = 32 - prefix;
		size = BigInteger.valueOf(2).pow(host).longValue();
		long address = IPv4Util.toLong(parts[0]);
		network = address >>> host << host;
		broadcast = network | (0xff_ff_ff_ff << prefix >>> prefix);
	}

	public static IPv4Block of(String block) {
		return new IPv4Block(block);
	}

	public IPv4 getNetwork() {
		return IPv4.of(network);
	}

	public IPv4 getBroadcast() {
		return IPv4.of(broadcast);
	}

	public int getPrefix() {
		return prefix;
	}

	public long getSize() {
		return size;
	}

	public void rewind() {
		current = network;
	}

	public IPv4 getNext() {
		return current > broadcast ? null : IPv4.of(current++);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (broadcast ^ (broadcast >>> 32));
		result = prime * result + (int) (current ^ (current >>> 32));
		result = prime * result + (int) (network ^ (network >>> 32));
		result = prime * result + prefix;
		result = prime * result + (int) (size ^ (size >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IPv4Block other = (IPv4Block) obj;
		if (broadcast != other.broadcast)
			return false;
		if (current != other.current)
			return false;
		if (network != other.network)
			return false;
		if (prefix != other.prefix)
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "IPv4Block [network=" + IPv4Util.toString(network) + ", broadcast=" + IPv4Util.toString(broadcast) + ", prefix=" + prefix + ", size=" + size + ", current=" + IPv4Util.toString(current) + "]";
	}
}
