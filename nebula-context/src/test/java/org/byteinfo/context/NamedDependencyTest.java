package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NamedDependencyTest {
	@Test
	void namedInstanceWithModule() {
		Context context = new Context(new HelloWorldModule());
		assertEquals("Hello!", context.instance(Key.of(String.class, "hello")));
		assertEquals("Hi!", context.instance(Key.of(String.class, "hi")));
	}

	public static class HelloWorldModule {
		@Provides
		@Named("hello")
		String hello() {
			return "Hello!";
		}

		@Provides
		@Named("hi")
		String hi() {
			return "Hi!";
		}
	}
}
