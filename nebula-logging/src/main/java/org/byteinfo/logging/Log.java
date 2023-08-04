package org.byteinfo.logging;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Supplier;

public final class Log {
	private static volatile Level level;
	private static volatile Writer writer;

	static {
		try {
			Properties config = new Properties();
			config.load(ClassLoader.getSystemResourceAsStream("org/byteinfo/logging/logging.properties"));
			InputStream in = ClassLoader.getSystemResourceAsStream("logging.properties");
			if (in != null) {
				config.load(in);
			}
			String configString = System.getProperty("logging");
			if (configString != null) {
				for (String optionStr : configString.split("[:;]")) {
					String[] option = optionStr.split("=", 2);
					config.setProperty(option[0], option[1]);
				}
			}
			level = Level.valueOf(config.getProperty("level").toUpperCase());
			String output = config.getProperty("output");
			switch (output) {
				case "stdout" -> writer = new ConsoleWriter(System.out);
				case "stderr" -> writer = new ConsoleWriter(System.err);
				default -> {
					Path path = Path.of(output);
					Rolling rolling = (Rolling) Rolling.class.getDeclaredField(config.getProperty("rolling").toUpperCase()).get(null);
					int backups = Integer.parseInt(config.getProperty("backups"));
					writer = new FileWriter(path, rolling, backups);
				}
			}
		} catch (Exception e) {
			throw new LogException("Failed to load logging config", e);
		}
	}

	private Log() {
	}

	public static void setLevel(Level level) {
		Log.level = level;
	}

	public static void setWriter(Writer writer) {
		Log.writer = writer;
	}

	public static void shutdown() {
		try {
			writer.close();
		} catch (Exception e) {
			throw new LogException("Failed to close Writer: " + writer.getClass().getName(), e);
		}
	}

	public static boolean isLoggable(Level target) {
		return target.ordinal() >= level.ordinal();
	}

	/**
	 * Logs a message with an optional list of parameters.
	 *
	 * @param message the string message with optional "{}" placeholders.
	 * @param params an optional list of parameters to the message.
	 */
	public static void trace(String message, Object... params) {
		dispatch(Level.TRACE, message, params, null, null);
	}

	public static void debug(String message, Object... params) {
		dispatch(Level.DEBUG, message, params, null, null);
	}

	public static void info(String message, Object... params) {
		dispatch(Level.INFO, message, params, null, null);
	}

	public static void warn(String message, Object... params) {
		dispatch(Level.WARN, message, params, null, null);
	}

	public static void error(String message, Object... params) {
		dispatch(Level.ERROR, message, params, null, null);
	}

	/**
	 * Logs a lazily supplied message.
	 *
	 * @param supplier a supplier function that produces a message.
	 */
	public static void trace(Supplier<String> supplier) {
		dispatch(Level.TRACE, null, null, supplier, null);
	}

	public static void debug(Supplier<String> supplier) {
		dispatch(Level.DEBUG, null, null, supplier, null);
	}

	public static void info(Supplier<String> supplier) {
		dispatch(Level.INFO, null, null, supplier, null);
	}

	public static void warn(Supplier<String> supplier) {
		dispatch(Level.WARN, null, null, supplier, null);
	}

	public static void error(Supplier<String> supplier) {
		dispatch(Level.ERROR, null, null, supplier, null);
	}

	/**
	 * Logs a message of a given throwable.
	 *
	 * @param t a throwable associated with log message.
	 */
	public static void trace(Throwable t) {
		dispatch(Level.TRACE, null, null, null, t);
	}

	public static void debug(Throwable t) {
		dispatch(Level.DEBUG, null, null, null, t);
	}

	public static void info(Throwable t) {
		dispatch(Level.INFO, null, null, null, t);
	}

	public static void warn(Throwable t) {
		dispatch(Level.WARN, null, null, null, t);
	}

	public static void error(Throwable t) {
		dispatch(Level.ERROR, null, null, null, t);
	}

	/**
	 * Logs a given throwable and a message with an optional list of parameters.
	 *
	 * @param t a throwable associated with the log message.
	 * @param message the string message with optional "{}" placeholders.
	 * @param params an optional list of parameters to the message (may be none).
	 */
	public static void trace(Throwable t, String message, Object... params) {
		dispatch(Level.TRACE, message, params, null, t);
	}

	public static void debug(Throwable t, String message, Object... params) {
		dispatch(Level.DEBUG, message, params, null, t);
	}

	public static void info(Throwable t, String message, Object... params) {
		dispatch(Level.INFO, message, params, null, t);
	}

	public static void warn(Throwable t, String message, Object... params) {
		dispatch(Level.WARN, message, params, null, t);
	}

	public static void error(Throwable t, String message, Object... params) {
		dispatch(Level.ERROR, message, params, null, t);
	}

	/**
	 * Logs a lazily supplied message associated with a given throwable.
	 *
	 * @param t a throwable associated with log message.
	 * @param supplier a supplier function that produces a message.
	 */
	public static void trace(Throwable t, Supplier<String> supplier) {
		dispatch(Level.TRACE, null, null, supplier, t);
	}

	public static void debug(Throwable t, Supplier<String> supplier) {
		dispatch(Level.DEBUG, null, null, supplier, t);
	}

	public static void info(Throwable t, Supplier<String> supplier) {
		dispatch(Level.INFO, null, null, supplier, t);
	}

	public static void warn(Throwable t, Supplier<String> supplier) {
		dispatch(Level.WARN, null, null, supplier, t);
	}

	public static void error(Throwable t, Supplier<String> supplier) {
		dispatch(Level.ERROR, null, null, supplier, t);
	}

	/**
	 * <em>INTERNAL USE ONLY.<em/> Dispatches log messages.
	 *
	 * @param level log message level
	 * @param message the string message with optional "{}" placeholders.
	 * @param params an optional list of parameters to the message.
	 * @param supplier a supplier function that produces a message.
	 * @param throwable a throwable associated with log message.
	 */
	public static void dispatch(Level level, String message, Object[] params, Supplier<String> supplier, Throwable throwable) {
		if (isLoggable(level)) {
			try {
				writer.write(System.currentTimeMillis(), level, Thread.currentThread().getName(), message, params, supplier, throwable);
			} catch (Exception e) {
				throw new LogException("Failed to write message to Writer: " + writer.getClass().getName(), e);
			}
		}
	}
}
