package org.byteinfo.util.function;

/**
 * CheckedBiConsumer
 */
@FunctionalInterface
public interface CheckedBiConsumer<T, U> {
	void accept(T t, U u) throws Exception;
}
