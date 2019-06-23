package org.byteinfo.util.function;

/**
 * CheckedSupplier
 */
@FunctionalInterface
public interface CheckedSupplier<T> {
	T get() throws Exception;
}
