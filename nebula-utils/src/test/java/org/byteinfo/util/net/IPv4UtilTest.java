package org.byteinfo.util.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IPv4UtilTest {
	@Test
	void testIPv4UtilTest() {
		// https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing#CIDR_blocks
		byte[] byteAddr = new byte[] {(byte) 0b11010000, (byte) 0b10000010, 0b00011101, 0b00100001};
		int intAddr = 0b11010000_10000010_00011101_00100001;
		long longAddr = 0b11010000_10000010_00011101_00100001L;
		String stringAddr = "208.130.29.33";

		assertArrayEquals(byteAddr, IPv4Util.toBytes(intAddr));
		assertArrayEquals(byteAddr, IPv4Util.toBytes(longAddr));
		assertArrayEquals(byteAddr, IPv4Util.toBytes(stringAddr));

		assertEquals(intAddr, IPv4Util.toInt(byteAddr));
		assertEquals(intAddr, IPv4Util.toInt(longAddr));
		assertEquals(intAddr, IPv4Util.toInt(stringAddr));

		assertEquals(longAddr, IPv4Util.toLong(byteAddr));
		assertEquals(longAddr, IPv4Util.toLong(intAddr));
		assertEquals(longAddr, IPv4Util.toLong(stringAddr));

		assertEquals(stringAddr, IPv4Util.toString(byteAddr));
		assertEquals(stringAddr, IPv4Util.toString(intAddr));
		assertEquals(stringAddr, IPv4Util.toString(longAddr));
	}
}
