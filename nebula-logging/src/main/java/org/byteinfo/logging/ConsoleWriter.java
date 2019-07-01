package org.byteinfo.logging;

import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

public class ConsoleWriter implements Writer {
	private final Level current;

	public ConsoleWriter(Level level) {
		current = level;
	}

	@Override
	public void write(int offset, long time, DateTimeFormatter formatter, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) throws Exception {
		if (isEnabled(level, current)) {
			System.out.println(format(offset, time, formatter, level, thread, message, params, supplier, throwable));
		}
	}
}
