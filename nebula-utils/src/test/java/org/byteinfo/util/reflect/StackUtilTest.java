package org.byteinfo.util.reflect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StackUtilTest {
	@Test
	void testStackUtil() {
		// reflection frames will be ignored
		Assertions.assertNotSame(StackUtil.WALKER.getCallerClass(), StackUtilTest.class);
		Assertions.assertSame(StackUtil.getStack(0).getDeclaringClass(), StackUtil.class);
		Assertions.assertSame(StackUtil.getStack(1).getDeclaringClass(), StackUtilTest.class);
		Assertions.assertSame(StackUtil.getStack(2).getDeclaringClass(), StackUtil.WALKER.getCallerClass());
		Assertions.assertSame(StackUtil.getCurrentStack().getDeclaringClass(), StackUtilTest.class);
		testNonReflectMethodCall();
	}

	private void testNonReflectMethodCall() {
		Assertions.assertSame(StackUtil.WALKER.getCallerClass(), StackUtilTest.class);
		Assertions.assertSame(StackUtil.getStack(0).getDeclaringClass(), StackUtil.class);
		Assertions.assertSame(StackUtil.getStack(1).getDeclaringClass(), StackUtilTest.class);
		Assertions.assertSame(StackUtil.getStack(2).getDeclaringClass(), StackUtil.WALKER.getCallerClass());
		Assertions.assertSame(StackUtil.getCurrentStack().getDeclaringClass(), StackUtilTest.class);
	}
}
