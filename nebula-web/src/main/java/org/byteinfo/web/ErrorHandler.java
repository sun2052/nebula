package org.byteinfo.web;

@FunctionalInterface
public interface ErrorHandler {
	Object handle(HttpContext ctx, Throwable t) throws Exception;
}
