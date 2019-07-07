package org.byteinfo.web;

/**
 * HTTP Interceptor
 */
public interface Interceptor {
	default boolean before(Request request, Response response, Handler handler) throws Exception {
		return true; // continue pending execution
	}

	default boolean after(Request request, Response response, Handler handler) throws Exception {
		return true; // continue pending execution
	}

	default void complete(Request request, Response response, Handler handler, Exception ex) throws Exception {
	}
}
