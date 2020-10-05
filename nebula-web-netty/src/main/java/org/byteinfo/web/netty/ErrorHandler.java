package org.byteinfo.web.netty;

/**
 * Error Handler
 */
@FunctionalInterface
public interface ErrorHandler {
	Object handle(HttpContext context, Exception ex) throws Exception;
}
