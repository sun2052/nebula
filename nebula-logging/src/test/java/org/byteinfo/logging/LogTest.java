package org.byteinfo.logging;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogTest {
	@Test
	void testLog() {
		Log.trace("Hello World.");
		Log.debug("Hello World.");
		Log.info("Hello {}.", "World");
		Log.warn(() -> "Hello World.");
		Log.error(new Throwable(), "Hello World with Exception:");
	}

	@Test
	void testSlf4j() {
		Logger log = LoggerFactory.getLogger(LogTest.class);
		log.trace("Hello World.");
		log.debug("Hello World.");
		log.info("Hello {}.", "World");
		log.warn("Hello World.");
		log.error("Hello World with Exception:", new Throwable());
	}
}
