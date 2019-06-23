package org.byteinfo.util.function;

/**
 * CheckedConsumer
 */
@FunctionalInterface
public interface CheckedConsumer<T> {
	void accept(T t) throws Exception;
}
