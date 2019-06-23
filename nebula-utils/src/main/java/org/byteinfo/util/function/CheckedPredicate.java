package org.byteinfo.util.function;

/**
 * CheckedPredicate
 */
@FunctionalInterface
public interface CheckedPredicate<T> {
	boolean test(T t) throws Exception;
}
