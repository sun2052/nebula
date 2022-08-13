package org.byteinfo.util.text;

/**
 * StringUtil
 */
public interface StringUtil {
	static String strip(String target) {
		return target == null ? null : target.strip();
	}

	static boolean isEmpty(String target) {
		return target == null || target.length() == 0;
	}

	static boolean isNotEmpty(String target) {
		return target != null && target.length() > 0;
	}

	static boolean isBlank(String target) {
		return target == null || target.strip().length() == 0;
	}

	static boolean isNotBlank(String target) {
		return target != null && target.strip().length() > 0;
	}
}
