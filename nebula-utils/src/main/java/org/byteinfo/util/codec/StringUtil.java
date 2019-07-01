package org.byteinfo.util.codec;

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

	/**
	 * Format Message
	 *
	 * @param message message with optional {} placeholder
	 * @param params optional parameters
	 * @return formatted message
	 */
	static StringBuilder format(String message, Object[] params) {
		StringBuilder builder = new StringBuilder(256);
		if (message != null) {
			if (params == null || params.length == 0) {
				builder.append(message);
			} else {
				int index = 0;
				for (int i = 0; i < params.length && index < message.length(); i++) {
					int found = message.indexOf("{}", index);
					if (found >= 0) {
						builder.append(message, index, found);
						builder.append(params[i]);
						index = found + 2;
					} else {
						break;
					}
				}
				builder.append(message, index, message.length());
			}
		}
		return builder;
	}

}
