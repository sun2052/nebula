package org.byteinfo.util.text;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * StringUtilTest
 */
public class StringUtilTest {
	@Test
	void testStringUtil() {
		Assertions.assertNull(StringUtil.strip(null));
		Assertions.assertEquals("abc", StringUtil.strip("  \t  abc \t   \t  "));

		Assertions.assertTrue(StringUtil.isEmpty(null));
		Assertions.assertTrue(StringUtil.isEmpty(""));
		Assertions.assertFalse(StringUtil.isEmpty("abc"));

		Assertions.assertFalse(StringUtil.isNotEmpty(null));
		Assertions.assertFalse(StringUtil.isNotEmpty(""));
		Assertions.assertTrue(StringUtil.isNotEmpty("abc"));

		Assertions.assertTrue(StringUtil.isBlank(null));
		Assertions.assertTrue(StringUtil.isBlank(""));
		Assertions.assertTrue(StringUtil.isBlank("   \t "));
		Assertions.assertFalse(StringUtil.isBlank("abc"));

		Assertions.assertFalse(StringUtil.isNotBlank(null));
		Assertions.assertFalse(StringUtil.isNotBlank(""));
		Assertions.assertFalse(StringUtil.isNotBlank("   \t "));
		Assertions.assertTrue(StringUtil.isNotBlank("abc"));
	}
}
