package org.byteinfo.web;

@FunctionalInterface
public interface Handler {
	Object handle(HttpContext ctx) throws Exception;
}