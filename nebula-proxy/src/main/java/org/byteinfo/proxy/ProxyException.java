package org.byteinfo.proxy;

public class ProxyException extends RuntimeException {
	public ProxyException() {
	}

	public ProxyException(String message) {
		super(message);
	}

	public ProxyException(Throwable cause) {
		super(cause);
	}

	public ProxyException(String message, Throwable cause) {
		super(message, cause);
	}
}
