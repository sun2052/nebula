package org.byteinfo.context;

/**
 * TypeProcessor
 */
@FunctionalInterface
public interface TypeProcessor {
	/**
	 * Process the target type before initialization.
	 *
	 * @param clazz target type
	 * @return the type to use, either the original or a proxy
	 */
	Class<?> process(Class<?> clazz);
}
