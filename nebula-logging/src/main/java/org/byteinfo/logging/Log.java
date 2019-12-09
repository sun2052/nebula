package org.byteinfo.logging;

import org.byteinfo.util.io.IOUtil;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

public class Log {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());
	private static final Level MIN_LEVEL;
	private static final Writer[] WRITERS;

	static {
		try {
			Properties props = new Properties();
			try (InputStream in = IOUtil.getClassResource("org/byteinfo/logging/logging.properties").openStream()) {
				props.load(in);
			}
			URL resource = IOUtil.getClassResource("logging.properties");
			if (resource != null) {
				try (InputStream in = resource.openStream()) {
					if (in != null) {
						props.load(in);
					}
				}
			}

			Level minLevel = Level.OFF;
			List<Writer> writers = new ArrayList<>();
			for (String name : props.stringPropertyNames()) {
				if (!name.contains(".")) {
					Level level = Level.valueOf(props.getProperty(name + ".level"));
					if (level.ordinal() < minLevel.ordinal()) {
						minLevel = level;
					}
					String type = props.getProperty(name);
					if ("console".equals(type)) {
						writers.add(new ConsoleWriter(level));
					} else if ("file".equals(type)) {
						Path target = Path.of(props.getProperty(name + ".path"));
						Rolling rolling = (Rolling) Rolling.class.getDeclaredField(props.getProperty(name + ".rolling")).get(null);
						int backups = Integer.parseInt(props.getProperty(name + ".backups"));
						writers.add(new FileWriter(level, target, rolling, backups));
					} else {
						throw new LogException("Unsupported Writer type: " + type);
					}
				}
			}
			MIN_LEVEL = minLevel;
			WRITERS = writers.toArray(new Writer[0]);

			Runtime.getRuntime().addShutdownHook(new Thread(Log::shutdown));
		} catch (Exception e) {
			throw new LogException("Failed to initialize Nebula Logging.", e);
		}
	}

	private Log() {
	}

	public static void shutdown() {
		for (Writer writer : WRITERS) {
			try {
				writer.close();
			} catch (Exception e) {
				throw new LogException("Failed to close Writer: " + writer.getClass().getName(), e);
			}
		}
	}

	public static boolean isLoggable(Level target) {
		return target.ordinal() >= MIN_LEVEL.ordinal();
	}

	/**
	 * Logs a message with an optional list of parameters.
	 *
	 * @param message the string message with optional "{}" placeholders.
	 * @param params an optional list of parameters to the message (may be none).
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
			long time = System.currentTimeMillis();
			for (Writer writer : WRITERS) {
				try {
					writer.write(0, time, FORMATTER, level, Thread.currentThread().getName(), message, params, supplier, throwable);
				} catch (Exception e) {
					throw new LogException("Failed to write message to Writer: " + writer.getClass().getName(), e);
				}
			}
		}
	}
}
