package org.byteinfo.web;

/**
 * HTTP Handler
 */
@FunctionalInterface
public interface Handler {
	void handle(Request request, Response response) throws Exception;
}
