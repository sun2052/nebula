package org.byteinfo.proxy;

public class ProxyTarget {
	public static Object proceed() {
		throw new ProxyException();
	}

	public static String name() {
		throw new ProxyException();
	}

	public static int argumentCount() {
		throw new ProxyException();
	}

	public static Class<?>[] argumentTypes() {
		throw new ProxyException();
	}

	public static Class<?> returnType() {
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
