package org.byteinfo.proxy;

import jdk.internal.classfile.MethodModel;

@FunctionalInterface
public interface Pointcut {
	boolean apply(MethodModel methodModel);
}
