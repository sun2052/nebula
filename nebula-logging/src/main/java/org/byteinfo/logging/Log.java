package org.byteinfo.logging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.function.Supplier;

public final class Log {
	private static volatile Level level = Level.INFO;
	private static volatile Writer writer;

	static {
		applyConfig(System.getProperty("nlog"));
	}

	private Log() {
	}

	/**
	 * Parses and apply the specified config string.
	 *
	 * <pre>
	 * [output[:options]]
	 * [output[;options]]
	 *
	 * output: stdout, stderr, /path/to/file.{}.log
	 * options: level=info[,rolling=daily[,backups=60]]
	 * level = trace, debug, info, warn, error, off
	 * rolling = none, daily, monthly
	 * backups = max number of old log files to keep
	 *
	 * default:
	 * output = stdout
	 * level = info
	 * rolling = none, file output only
	 * backups = 30, file output only
	 * </pre>
	 *
	 * @param config the config string
	 */
	public static void applyConfig(String config) {
		if (config == null) {
			level = Level.INFO;
			writer = new ConsoleWriter(System.out);
		} else {
			String[] parts = config.split("[:;]");
			if (parts.length > 2) {
				throw new LogException("invalid config: " + config + ", expected: output[:options]]");
			}

			Rolling rolling = Rolling.NONE;
			int backups = 0;
			if (parts.length == 2) {
				for (String optionStr : parts[1].split(",")) {
					String[] option = optionStr.split("=", 2);
					if (option.length != 2) {
						throw new LogException("invalid config option: " + optionStr + ", expected: level=info[,rolling=daily[,backups=60]]");
					}
					switch (option[0]) {
						case "level" -> {
							var levelOption = Arrays.stream(Level.values()).filter(value -> value.name().equalsIgnoreCase(option[1])).findFirst();
							if (levelOption.isPresent()) {
								level = levelOption.get();
							} else {
								throw new LogException("invalid level option: " + optionStr + ", expected: [trace, debug, info, warn, error, off]");
							}
						}
						case "rolling" -> {
							try {
								rolling = (Rolling) Rolling.class.getDeclaredField(option[1].toUpperCase()).get(null);
							} catch (Exception e) {
								throw new LogException("invalid rolling option: " + optionStr + ", expected: [none, daily, monthly]");
							}
						}
						case "backups" -> {
							try {
								backups = Integer.parseInt(option[1]);
							} catch (Exception e) {
								throw new LogException("invalid backups option: " + optionStr + ", expected: >= 0");
							}
						}
						default -> throw new LogException("unknown config option: " + optionStr + ", expected: level=info[,rolling=daily[,backups=60]]");
					}
				}
			}

			switch (parts[0]) {
				case "stdout" -> writer = new ConsoleWriter(System.out);
				case "stderr" -> writer = new ConsoleWriter(System.err);
				default -> {
					Path path = Path.of(parts[0]);
					if (Files.notExists(path.getParent())) {
						throw new LogException("invalid output config: " + parts[0] + ", parent path does not exist");
					}
					writer = new FileWriter(path, rolling, backups);
				}
			}
		}
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
			try {
				writer.write(System.currentTimeMillis(), level, Thread.currentThread().getName(), message, params, supplier, throwable);
			} catch (Exception e) {
				throw new LogException("Failed to write message to Writer: " + writer.getClass().getName(), e);
			}
		}
	}
}
