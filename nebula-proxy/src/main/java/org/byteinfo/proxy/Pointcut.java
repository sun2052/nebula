package org.byteinfo.proxy;

@FunctionalInterface
public interface Pointcut {
	boolean apply(MethodInfo methodInfo);
}
