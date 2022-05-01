package org.byteinfo.util.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class RandomUtilTest {
	@Test
	void test() {
		assertNotEquals(RandomUtil.getNumeric(10), RandomUtil.getNumeric(10));
		assertNotEquals(RandomUtil.getAlphabetic(10), RandomUtil.getAlphabetic(10));
		assertNotEquals(RandomUtil.getAlphaNumeric(10), RandomUtil.getAlphaNumeric(10));
	}
}
