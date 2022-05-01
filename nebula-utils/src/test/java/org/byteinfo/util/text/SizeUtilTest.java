package org.byteinfo.util.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SizeUtilTest {
	@Test
	void test() {
		assertEquals(SizeUtil.toHumanReadable(0), "0 B");
		assertEquals(SizeUtil.toHumanReadable(1023), "1023 B");
		assertEquals(SizeUtil.toHumanReadable(1024), "1 KB");
		assertEquals(SizeUtil.toHumanReadable(1025), "1 KB");
		assertEquals(SizeUtil.toHumanReadable(1024L * 1024), "1 MB");
		assertEquals(SizeUtil.toHumanReadable(1024L * 1024 * 3), "3 MB");
		assertEquals(SizeUtil.toHumanReadable(1024L * 1024 * 1024 * 3 + 1024 * 1024 * 20), "3.02 GB");
	}
}