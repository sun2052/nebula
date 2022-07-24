package org.byteinfo.util.function;

/**
 * CheckedBiPredicate
 */
@FunctionalInterface
public interface CheckedBiPredicate<T, U> {
	boolean test(T t, U u) throws Exception;
}