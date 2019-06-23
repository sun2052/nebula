package org.byteinfo.util.function;

/**
 * CheckedFunction
 */
@FunctionalInterface
public interface CheckedFunction<T, R> {
	R apply(T t) throws Exception;
}
