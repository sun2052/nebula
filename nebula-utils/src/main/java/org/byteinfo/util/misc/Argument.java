package org.byteinfo.util.misc;

public interface Argument {

	static void require(boolean expression, String errorMessage, Object... errorMessageArgs) {
		if (!expression) {
			throw new IllegalArgumentException(String.format(errorMessage, errorMessageArgs));
		}
	}

}
