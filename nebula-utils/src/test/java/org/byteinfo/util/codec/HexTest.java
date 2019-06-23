package org.byteinfo.util.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HexTest {
	private byte[] byteArray = {1, 35, 69, 103, -119, -85, -51, -17};
	private String hexString = "0123456789abcdef";

	@Test
	void test() {
		assertEquals(hexString, Hex.encode(byteArray));
		assertArrayEquals(byteArray, Hex.decode(hexString));
	}
}
