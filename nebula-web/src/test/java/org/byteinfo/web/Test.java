package org.byteinfo.web;

import org.byteinfo.logging.Level;
import org.byteinfo.logging.Log;

public class Test {
	public static void main(String[] args) throws Exception {
		Log.setLevel(Level.TRACE);
		new Server().get("/", ctx -> {
			ctx.setResponseType(ContentType.TEXT);
			return "Hello, World!";
		}).start();
	}
}
