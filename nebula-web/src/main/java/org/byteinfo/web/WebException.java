package org.byteinfo.web;

public class WebException extends RuntimeException {
	private int status = StatusCode.INTERNAL_SERVER_ERROR;

	public WebException(int status, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

	public WebException(int status, String message) {
		super(message);
		this.status = status;
	}

	public WebException(int status, Throwable cause) {
		super(cause);
		this.status = status;
	}

	public WebException(int status) {
		this.status = status;
	}

	public WebException(String message) {
		super(message);
	}

	public WebException(Throwable cause) {
		super(cause);
	}

	public int getStatus() {
		return status;
	}
}
