package org.byteinfo.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public interface Writer extends AutoCloseable {
	DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

	/**
	 * Outputs a given message.
	 *
	 * @param time time in millis
	 * @param level logger level
	 * @param thread current thread name
	 * @param message message, may be null
	 * @param params params for the message, may be null
	 * @param supplier message supplier, may be null
	 * @param throwable throwable, may be null
	 * @throws Exception if an error occurs
	 */
	void write(long time, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) throws Exception;

	default void close() throws Exception {
	}

	/**
	 * Format Log
	 *
	 * @param time time in millis
	 * @param level logger level
	 * @param thread current thread name
	 * @param message message, may be null
	 * @param params params for the message, may be null
	 * @param supplier message supplier, may be null
	 * @param throwable throwable, may be null
	 * @return the formatted log message
	 */
	static String formatLog(long time, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) {
		StringBuilder builder = new StringBuilder(1024);

		// Date & Time
		builder.append(FORMATTER.format(Instant.ofEpochMilli(time)));
		builder.append(' ');

		// Level
		builder.append(level);
		builder.append(' ');

		// Thread
		builder.append('[');
		builder.append(thread);
		builder.append(']');
		builder.append(' ');

		// Class Name, Method Name and Line Number
		StackWalker.StackFrame stackFrame = StackWalker.getInstance().walk(s -> s.skip(4).findFirst()).orElse(null);
		if (stackFrame != null) {
			String className = stackFrame.getClassName();
			builder.append(className.substring(className.lastIndexOf('.') + 1));
			builder.append('.');
			builder.append(stackFrame.getMethodName());
			builder.append("()");
		}

		builder.append(": ");

		// Format message, if any
		StringBuilder formatted = formatMessage(message, params);
		if (formatted.length() != 0) {
			builder.append(formatted);
		} else if (supplier != null) {
			builder.append(supplier.get());
		}

		// Format throwable, if any
		if (throwable != null) {
			builder.append(System.lineSeparator());
			StringWriter sw = new StringWriter(1024);
			throwable.printStackTrace(new PrintWriter(sw));
			builder.append(sw);
		}

		return builder.toString();
	}

	/**
	 * Format Message
	 *
	 * @param message message with optional {} placeholder
	 * @param params optional parameters
	 * @return formatted message
	 */
	static StringBuilder formatMessage(String message, Object[] params) {
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
