package org.byteinfo.util.net;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IPv4BlockTest {
	@Test
	public void testIPv4Block() {
		// https://en.wikipedia.org/wiki/Classless_Inter-Domain_Routing#IPv4_CIDR_blocks
		String ipBlock = "208.130.29.33/31";

		IPv4Block block = IPv4Block.of(ipBlock);
		assertEquals("208.130.29.32", block.getNetwork().getString());
		assertEquals("208.130.29.33", block.getBroadcast().getString());
		assertEquals(31, block.getPrefix());
		assertEquals(2, block.getSize());
	}
}
