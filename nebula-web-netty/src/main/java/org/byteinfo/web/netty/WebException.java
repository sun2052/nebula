package org.byteinfo.web.netty;

import io.netty.handler.codec.http.HttpResponseStatus;

public class WebException extends RuntimeException {
	private HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;

	public WebException(String message) {
		super(message);
	}

	public WebException(String message, Throwable cause) {
		super(message, cause);
	}

	public WebException(HttpResponseStatus status) {
		this.status = status;
	}

	public WebException(HttpResponseStatus status, Throwable cause) {
		super(cause);
		this.status = status;
	}

	public WebException(Throwable cause) {
		super(cause);
	}

	public WebException(String message, HttpResponseStatus status) {
		this(message, status, null);
	}

	public WebException(String message, HttpResponseStatus status, Throwable cause) {
		super(message, cause);
		this.status = status;
	}

	public HttpResponseStatus getStatus() {
		return status;
	}
}
