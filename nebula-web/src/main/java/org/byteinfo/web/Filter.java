package org.byteinfo.web;

public interface Filter {
	default void before(HttpContext ctx, Handler handler) throws Exception {}

	default void after(HttpContext ctx, Handler handler, Object result) throws Exception {}

	default void complete(HttpContext ctx, Handler handler, Throwable t) throws Exception {}
}
