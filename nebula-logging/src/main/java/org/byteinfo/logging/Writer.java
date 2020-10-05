package org.byteinfo.logging;

import org.byteinfo.util.codec.StringUtil;
import org.byteinfo.util.reflect.StackUtil;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public interface Writer {
	/**
	 * Outputs a given message.
	 *
	 * @param offset stack trace offset
	 * @param time time in millis
	 * @param formatter DateTimeFormatter
	 * @param level logger level
	 * @param thread current thread name
	 * @param message message, may be null
	 * @param params params for the message, may be null
	 * @param supplier message supplier, may be null
	 * @param throwable throwable, may be null
	 * @throws Exception if an error occurs
	 */
	void write(int offset, long time, DateTimeFormatter formatter, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) throws Exception;

	default void close() throws Exception {
	}

	default boolean isEnabled(Level target, Level current) {
		return target.ordinal() >= current.ordinal();
	}

	default String format(int offset, long time, DateTimeFormatter formatter, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) {
		StringBuilder builder = new StringBuilder(1024);

		// Date & Time
		builder.append(formatter.format(Instant.ofEpochMilli(time)));
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
		StackWalker.StackFrame stackFrame = StackUtil.getStack(5 + offset);
		builder.append(stackFrame.getClassName());
		builder.append('.');
		builder.append(stackFrame.getMethodName());
		builder.append("#");
		builder.append(stackFrame.getLineNumber());

		builder.append(": ");

		// Format message, if any
		StringBuilder formatted = StringUtil.format(message, params);
		if (formatted.length() != 0) {
			builder.append(formatted);
		} else if (supplier != null) {
			builder.append(supplier.get());
		}

		// Format throwable, if any
		if (throwable != null) {
			builder.append(System.lineSeparator());
			StringWriter sw = new StringWriter(512);
			throwable.printStackTrace(new PrintWriter(sw));
			builder.append(sw);
		}

		return builder.toString();
	}
}
