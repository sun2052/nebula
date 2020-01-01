package org.byteinfo.proxy;

@FunctionalInterface
public interface Advice {
	Object apply() throws Exception;
}
