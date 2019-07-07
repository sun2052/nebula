package org.byteinfo.web;

import java.lang.reflect.Method;

public class MVCHandler implements Handler {
	private Object object;
	private Method method;

	public MVCHandler(Object object, Method method) {
		this.object = object;
		this.method = method;
		this.method.setAccessible(true);
	}

	@Override
	public void handle(Request request, Response response) throws Exception {
		response.result(method.invoke(object, request, response));
	}
}
