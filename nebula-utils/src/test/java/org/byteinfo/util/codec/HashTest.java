package org.byteinfo.util.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * HashTest
 */
public class HashTest {
	@Test
	public void testHash() {
		assertEquals("sQnzu7wkTrgkQZF-0G1hi5AI3Qmzvv0bXgc5THBqi7mAsdd4Xll27ASbRt9fEyavWi6m0QP9B8lThf-rDKy8hg", Hash.SHA512.toBase64("password"));
	}
}
