package org.byteinfo.web;

/**
 * HTTP Handler
 */
@FunctionalInterface
public interface Handler {
	Object handle(HttpContext context) throws Exception;
}
