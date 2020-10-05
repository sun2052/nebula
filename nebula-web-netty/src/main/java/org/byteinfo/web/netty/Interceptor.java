package org.byteinfo.web.netty;

/**
 * HTTP Interceptor
 */
public interface Interceptor {
	default void before(HttpContext context, Handler handler) throws Exception {}

	default void after(HttpContext context, Handler handler, Object result) throws Exception {}

	default void complete(HttpContext context, Handler handler, Exception ex) throws Exception {}
}
