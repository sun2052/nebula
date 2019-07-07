package org.byteinfo.web;

/**
 * Error Handler
 */
@FunctionalInterface
public interface ErrorHandler {
	void handle(Request request, Response response, Exception ex) throws Exception;
}
