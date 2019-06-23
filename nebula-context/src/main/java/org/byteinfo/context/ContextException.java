package org.byteinfo.context;

public class ContextException extends RuntimeException {
	public ContextException(String message) {
		super(message);
	}

	public ContextException(Throwable cause) {
		super(cause);
	}

	public ContextException(String message, Throwable cause) {
		super(message, cause);
	}
}
