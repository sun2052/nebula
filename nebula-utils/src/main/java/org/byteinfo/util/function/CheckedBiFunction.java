package org.byteinfo.util.function;

/**
 * CheckedBiFunction
 */
@FunctionalInterface
public interface CheckedBiFunction<T, U, R> {
	R apply(T t, U u) throws Exception;
}
