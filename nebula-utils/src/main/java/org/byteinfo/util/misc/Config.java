package org.byteinfo.util.misc;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Config
 */
public class Config {
	private final Properties properties = new Properties();

	/**
	 * Loads configurations
	 *
	 * @param config config file
	 * @throws IOException exception
	 */
	public void load(String config) throws IOException {
		boolean loaded = loadOptional(config);
		if (!loaded) {
			throw new IllegalArgumentException("Config not found: " + config);
		}
	}

	/**
	 * Loads optional configurations
	 *
	 * @param config config file
	 * @return if loaded
	 * @throws IOException exception
	 */
	public boolean loadOptional(String config) throws IOException {
		return load(ClassLoader.getSystemResourceAsStream(config));
	}

	/**
	 * Loads configurations
	 *
	 * @param stream input stream for config file
	 * @throws IOException exception
	 */
	public boolean load(InputStream stream) throws IOException {
		if (stream == null) {
			return false;
		}
		try (stream) {
			properties.load(stream);
		}
		return true;
	}

	/**
	 * Loads configurations
	 *
	 * @param config configuration
	 */
	public void load(Map<?, ?> config) {
		config.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
	}

	/**
	 * Resolves all variables.
	 */
	public void resolveAllVariables() {
		Pattern variablePattern = Pattern.compile("\\$\\{([^}]+)}");
		properties.forEach((name, value) -> resolveVariable((String) name, (String) value, variablePattern, new LinkedHashSet<>()));
	}

	/**
	 * Gets string value
	 *
	 * @param key config key
	 * @return target or null
	 */
	public String get(String key) {
		return properties.getProperty(key);
	}

	/**
	 * Gets string value with default
	 *
	 * @param key config key
	 * @param defaultValue default
	 * @return target or default
	 */
	public String get(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	/**
	 * Gets int value
	 *
	 * @param key config key
	 * @return target
	 * @throws NumberFormatException if value is not parsable
	 */
	public int getInt(String key) {
		return Integer.parseInt(get(key));
	}

	/**
	 * Gets int value with default
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
	 * Gets long value
	 *
	 * @param key config key
	 * @return target
	 */
	public long getLong(String key) {
		return Long.parseLong(get(key));
	}

	/**
	 * Gets long value with default
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
	 * Gets double value
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
	 * Gets double value with default
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
	 * Gets boolean value
	 *
	 * @param key config key
	 * @return target
	 */
	public boolean getBoolean(String key) {
		return Boolean.parseBoolean(get(key));
	}

	/**
	 * Gets all property names.
	 *
	 * @return unmodifiable set of property names
	 */
	public Set<String> getPropertyNames() {
		return properties.stringPropertyNames();
	}

	// resolve variable recursively
	private String resolveVariable(String key, String value, Pattern variablePattern, Set<String> variableChain) {
		Matcher matcher = variablePattern.matcher(value);
		while (matcher.find()) {
			variableChain.add(key);
			String variable = matcher.group(1);

			// check for circular reference
			if (variableChain.contains(variable)) {
				List<String> list = new ArrayList<>(variableChain.size() + 1);
				list.add(variable);
				throw new IllegalArgumentException("Circular reference: " + String.join(" -> ", list));
			}

			// check for undefined variable
			String target = properties.getProperty(variable);
			if (target == null) {
				throw new IllegalArgumentException("Variable undefined: " + variable);
			}

			// apply resolved variable
			value = value.replace("${" + variable + "}", resolveVariable(variable, target, variablePattern, variableChain));
			properties.put(key, value);
		}
		variableChain.clear();
		return value;
	}
}
