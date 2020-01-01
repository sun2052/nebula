package org.byteinfo.proxy;

import org.objectweb.asm.Type;

public class ProxyTarget {
	public static Object proceed() {
		throw new ProxyException();
	}

	public static String name() {
		throw new ProxyException();
	}

	public static int argumentsCount() {
		throw new ProxyException();
	}

	public static Type[] argumentTypes() {
		throw new ProxyException();
	}

	public static Type returnType() {
		throw new ProxyException();
	}

	public static Object argument(int index) {
		throw new ProxyException();
	}

	public static Object[] arguments() {
		throw new ProxyException();
	}

	public static void setArgument(Object value, int index) {
		throw new ProxyException();
	}
}
