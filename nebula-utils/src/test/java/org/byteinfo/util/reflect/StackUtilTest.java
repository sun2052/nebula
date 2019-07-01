package org.byteinfo.util.reflect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

public class StackUtilTest {
	@Test
	void testStackUtil() {
		assertSame(StackUtil.WALKER.getCallerClass(), StackUtil.getStack(2).getDeclaringClass());
	}
}
