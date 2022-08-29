package org.byteinfo.raft.socket;

public record Address(String host, int port) {
	public static Address of(String address) {
		String[] parts = address.split(":", 2);
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid address: " + address + ", expected: host:port");
		}
		return new Address(parts[0], Integer.parseInt(parts[1]));
	}

	@Override
	public String toString() {
		return host + ":" + port;
	}
}
