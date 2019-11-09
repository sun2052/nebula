package org.byteinfo.util.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IPv4Test {
	@Test
	void testIPv4() {
		// https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing#CIDR_blocks
		byte[] byteAddr = new byte[] {(byte) 0b11010000, (byte) 0b10000010, 0b00011101, 0b00100001};
		int intAddr = 0b11010000_10000010_00011101_00100001;
		long longAddr = 0b11010000_10000010_00011101_00100001L;
		String stringAddr = "208.130.29.33";

		assertEquals(IPv4.of(byteAddr), IPv4.of(intAddr));
		assertEquals(IPv4.of(intAddr), IPv4.of(longAddr));
		assertEquals(IPv4.of(longAddr), IPv4.of(stringAddr));
	}
}
