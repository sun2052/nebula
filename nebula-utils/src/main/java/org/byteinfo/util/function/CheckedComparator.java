package org.byteinfo.util.function;

/**
 * CheckedComparator
 */
@FunctionalInterface
public interface CheckedComparator<T> {
	int compare(T o1, T o2) throws Exception;
}