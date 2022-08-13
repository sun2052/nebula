package org.byteinfo.logging;

import java.io.PrintStream;
import java.util.function.Supplier;

public class ConsoleWriter implements Writer {
	private final PrintStream out;

	public ConsoleWriter(PrintStream out) {
		this.out = out;
	}

	@Override
	public void write(long time, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) throws Exception {
		out.println(Writer.formatLog(time, level, thread, message, params, supplier, throwable));
	}
}
