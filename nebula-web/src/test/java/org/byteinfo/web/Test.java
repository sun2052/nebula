package org.byteinfo.web;

public class Test {
	public static void main(String[] args) throws Exception {
		new Server().get("/", ctx -> {
			ctx.setResponseType(ContentType.TEXT);
			return "Hello, World!";
		}).start();
	}
}
