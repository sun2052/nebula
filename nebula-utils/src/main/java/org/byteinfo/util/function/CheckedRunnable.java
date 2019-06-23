package org.byteinfo.util.function;

/**
 * CheckedRunnable
 */
@FunctionalInterface
public interface CheckedRunnable {
	void run() throws Exception;
}
