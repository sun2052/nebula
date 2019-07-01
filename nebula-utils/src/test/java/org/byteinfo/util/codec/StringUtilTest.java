package org.byteinfo.util.codec;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * StringUtilTest
 */
public class StringUtilTest {
	@Test
	void testStringUtil() {
		assertNull(StringUtil.strip(null));
		assertEquals("abc", StringUtil.strip("  \t  abc \t   \t  "));

		assertTrue(StringUtil.isEmpty(null));
		assertTrue(StringUtil.isEmpty(""));
		assertFalse(StringUtil.isEmpty("abc"));

		assertFalse(StringUtil.isNotEmpty(null));
		assertFalse(StringUtil.isNotEmpty(""));
		assertTrue(StringUtil.isNotEmpty("abc"));

		assertTrue(StringUtil.isBlank(null));
		assertTrue(StringUtil.isBlank(""));
		assertTrue(StringUtil.isBlank("   \t "));
		assertFalse(StringUtil.isBlank("abc"));

		assertFalse(StringUtil.isNotBlank(null));
		assertFalse(StringUtil.isNotBlank(""));
		assertFalse(StringUtil.isNotBlank("   \t "));
		assertTrue(StringUtil.isNotBlank("abc"));
	}
}
