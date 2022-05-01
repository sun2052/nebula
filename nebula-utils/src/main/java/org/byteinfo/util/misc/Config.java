package org.byteinfo.util.misc;

import org.byteinfo.util.io.IOUtil;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Config
 */
public class Config {
	private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

	private final Properties properties = new Properties();

	/**
	 * Load configuration
	 *
	 * @param resource configuration
	 * @throws IOException exception
	 */
	public void load(String resource) throws IOException {
		properties.load(IOUtil.reader(resource));
	}

	/**
	 * Load configuration if exist
	 *
	 * @param resource configuration
	 * @return if loaded
	 * @throws IOException exception
	 */
	public boolean loadIf(String resource) throws IOException {
		Reader in = IOUtil.reader(resource);
		if (in != null) {
			properties.load(in);
			return true;
		}
		return false;
	}

	/**
	 * Load configuration
	 *
	 * @param reader configuration
	 * @throws IOException exception
	 */
	public void load(Reader reader) throws IOException {
		try (reader) {
			properties.load(reader);
		}
	}

	/**
	 * Load configuration
	 *
	 * @param config configuration
	 */
	public void load(Map<?, ?> config) {
		config.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
	}

	/**
	 * Interpolate variables in configuration
	 */
	public void interpolate() {
		properties.forEach((name, value) -> interpolateEntry((String) name, (String) value, new LinkedHashSet<>()));
	}

	// Interpolate variables recursively
	private String interpolateEntry(String key, String value, Set<String> chain) {
		Matcher matcher = VARIABLE_PATTERN.matcher(value);

		while (matcher.find()) {
			chain.add(key);
			String variable = matcher.group(1);

			// checking for circular reference
			if (chain.contains(variable)) {
				StringBuilder chainString = new StringBuilder();
				chain.forEach(name -> chainString.append(name).append(" -> "));
				throw new IllegalArgumentException("Circular Reference Detected: " + chainString.append(variable));
			}

			String target = properties.getProperty(variable);
			if (target == null) {
				throw new IllegalArgumentException("Variable[" + variable + "] Not Found.");
			}

			value = value.replace("${" + variable + "}", interpolateEntry(variable, target, chain));
			properties.put(key, value);
		}

		chain.clear();

		return value;
	}

	/**
	 * Get string value
	 *
	 * @param key config key
	 * @return target or null
	 */
	public String get(String key) {
		return properties.getProperty(key);
	}

	/**
	 * Get string value with default
	 *
	 * @param key config key
	 * @param defaultValue default
	 * @return target or default
	 */
	public String get(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	/**
	 * Get int value
	 *
	 * @param key config key
	 * @return target
	 * @throws NumberFormatException if value is not parsable
	 */
	public int getInt(String key) {
		return Integer.parseInt(get(key));
	}

	/**
	 * Get int value with default
	 *
	 * @param key config key
	 * @param defaultValue default
	 * @return target or default
	 */
	public int getInt(String key, int defaultValue) {
		String value = get(key);
		if (value != null) {
			try {
				return Integer.parseInt(value);
			} catch (Exception e) {
				// ignore
			}
		}
		return defaultValue;
	}

	/**
	 * Get long value
	 *
	 * @param key config key
	 * @return target
	 */
	public long getLong(String key) {
		return Long.parseLong(get(key));
	}

	/**
	 * Get long value with default
	 *
	 * @param key config key
	 * @return target
	 * @throws NumberFormatException if value is not parsable
	 */
	public long getLong(String key, long defaultValue) {
		String value = get(key);
		if (value != null) {
			try {
				return Long.parseLong(value);
			} catch (Exception e) {
				// ignore
			}
		}
		return defaultValue;
	}

	/**
	 * Get double value
	 *
	 * @param key config key
	 * @return target
	 * @throws NullPointerException if value is null
	 * @throws NumberFormatException if value is not parsable
	 */
	public double getDouble(String key) {
		return Double.parseDouble(get(key));
	}

	/**
	 * Get double value with default
	 *
	 * @param key config key
	 * @return target or default
	 */
	public double getDouble(String key, double defaultValue) {
		String value = get(key);
		if (value != null) {
			try {
				return Double.parseDouble(value);
			} catch (Exception e) {
				// ignore
			}
		}
		return defaultValue;
	}

	/**
	 * Get boolean value
	 *
	 * @param key config key
	 * @return target
	 */
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(get(key));
	}

	/**
	 * Get all property names.
	 *
	 * @return unmodifiable set of property names
	 */
	public Set<String> getPropertyNames() {
		return properties.stringPropertyNames();
	}
}
