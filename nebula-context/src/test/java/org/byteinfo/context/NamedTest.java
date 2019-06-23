package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NamedTest {
	@Test
	void test() {
		Context context = new Context(new Module());
		assertEquals("Hi!", context.instance(Key.of(String.class, "hi")));
		assertEquals("Hello!", context.instance(Key.of(String.class, "hello")));
	}

	private static class Module {
		@Provides
		@Named("hi")
		String hi() {
			return "Hi!";
		}

		@Provides
		@Named("hello")
		String hello() {
			return "Hello!";
		}
	}
}
