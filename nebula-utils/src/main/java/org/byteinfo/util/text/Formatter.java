package org.byteinfo.util.text;

import java.util.function.Supplier;

/**
 * Handles converting objects to strings when rendering templates.
 */
@FunctionalInterface
public interface Formatter {

	/**
	 * Converts {@code value} to a string for inclusion in a template.
	 */
	String format(Object value);

	/**
	 * Default Formatter
	 */
	Formatter DEFAULT = value -> {
		Object target = value;
		if (value instanceof Supplier<?> supplier) {
			target = supplier.get();
		}
		return String.valueOf(target);
	};

}
