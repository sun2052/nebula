package org.byteinfo.proxy;

import java.lang.classfile.MethodModel;

@FunctionalInterface
public interface Pointcut {
	boolean apply(MethodModel methodModel);
}
