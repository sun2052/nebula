package org.byteinfo.util.codec;

import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HashTest
 */
public class HashTest {
	@Test
	public void testHash() throws NoSuchAlgorithmException {
		assertEquals("XohImNooBHFR0OVvjcYpJ3NgPQ1qq73WKhHvch0VQtg", Hash.hash("password"));
	}
}
