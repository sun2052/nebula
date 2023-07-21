package org.byteinfo.util.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeUtilTest {
	@Test
	void test() {
		assertEquals("0 B", SizeUtil.toHumanReadable(0));
		assertEquals("1023 B", SizeUtil.toHumanReadable(1023));
		assertEquals("1 KB", SizeUtil.toHumanReadable(1024));
		assertEquals("1 KB", SizeUtil.toHumanReadable(1025));
		assertEquals("1 MB", SizeUtil.toHumanReadable(1024L * 1024));
		assertEquals("3.02 GB", SizeUtil.toHumanReadable(1024L * 1024 * 1024 * 3 + 1024 * 1024 * 20));
	}
}