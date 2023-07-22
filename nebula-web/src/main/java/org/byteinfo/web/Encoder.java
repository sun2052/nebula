package org.byteinfo.web;

import java.io.IOException;
import java.io.InputStream;

@FunctionalInterface
public interface Encoder {
	Encoder DEFAULT = (ctx, result) -> {
		throw new IllegalArgumentException("Unsupported result type: " + result);
	};

	InputStream encode(HttpContext ctx, Object result) throws IOException;
}