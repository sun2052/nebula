package org.byteinfo.util.text;

public interface StringUtil {
	static boolean isEmpty(String str) {
		return str == null || str.length() == 0;
	}

	static boolean isBlank(String str) {
		return str == null || str.strip().length() == 0;
	}

	static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

	static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	static boolean isAnyEmpty(String... strs) {
		for (String str : strs) {
			if (isEmpty(str)) {
				return true;
			}
		}
		return false;
	}

	static boolean isAnyBlank(String... strs) {
		for (String str : strs) {
			if (isBlank(str)) {
				return true;
			}
		}
		return false;
	}

	static boolean isAllEmpty(String... strs) {
		for (String str : strs) {
			if (isNotEmpty(str)) {
				return false;
			}
		}
		return true;
	}

	static boolean isAllBlank(String... strs) {
		for (String str : strs) {
			if (isNotBlank(str)) {
				return false;
			}
		}
		return true;
	}

	static boolean isNoneEmpty(String... strs) {
		return !isAnyEmpty(strs);
	}

	static boolean isNoneBlank(String... strs) {
		return !isAnyBlank(strs);
	}

	static String truncate(String str, int maxLength) {
		return truncate(str, 0, maxLength);
	}

	static String truncate(String str, int offset, int maxLength) {
		if (str == null || str.isEmpty()) {
			return str;
		}
		if (offset >= str.length()) {
			return "";
		}
		if (str.length() > maxLength) {
			int endIndex = Math.min(offset + maxLength, str.length());
			return str.substring(offset, endIndex);
		}
		return str.substring(offset);
	}
}
