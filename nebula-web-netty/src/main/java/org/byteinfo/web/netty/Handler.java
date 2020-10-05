package org.byteinfo.web.netty;

/**
 * HTTP Handler
 */
@FunctionalInterface
public interface Handler {
	Object handle(HttpContext context) throws Exception;
}
