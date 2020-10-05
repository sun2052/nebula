package org.byteinfo.web;

import java.lang.reflect.Method;

public record MVCHandler(Object object, Method method) implements Handler {
	public MVCHandler {
		method.setAccessible(true);
	}

	@Override
	public Object handle(HttpContext context) throws Exception {
		return method.invoke(object, context);
	}
}
