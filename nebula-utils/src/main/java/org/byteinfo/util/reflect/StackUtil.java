package org.byteinfo.util.reflect;

/**
 * StackUtil
 */
public interface StackUtil {
	/**
	 * StackWalker
	 */
	StackWalker WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

	/**
	 * Get current StackFrame
	 *
	 * @return Current StackFrame
	 */
	static StackWalker.StackFrame getCurrentStack() {
		return getStack(2);
	}

	/**
	 * Get Specified StackFrame
	 *
	 * @param index StackFrame index
	 * @return StackFrame
	 */
	static StackWalker.StackFrame getStack(int index) {
		return WALKER.walk(s -> s.skip(index).findFirst()).orElse(null);
	}
}
