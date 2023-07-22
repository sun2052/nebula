package org.byteinfo.web;

import org.byteinfo.logging.Log;

@FunctionalInterface
public interface ErrorHandler {
	ErrorHandler DEFAULT = (ctx, t) -> {
		if (t instanceof WebException e) {
			ctx.setResponseStatus(e.getStatus());
		} else {
			ctx.setResponseStatus(StatusCode.INTERNAL_SERVER_ERROR);
		}
		Log.error(t, "Failed to handle request: {}: {} {} {}", ctx.id(), ctx.method(), ctx.target(), ctx.socket());
		return "ERROR: " + ctx.responseStatus();
	};

	Object handle(HttpContext ctx, Throwable t) throws Exception;
}
