package org.byteinfo.web.netty;

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
	public Object handle(HttpContext context) throws Exception {
		return method.invoke(object, context);
	}
}
