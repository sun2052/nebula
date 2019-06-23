package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QualifierTest {
	@Test
	void test() {
		Context context = new Context(new Module());
		assertEquals("Hi!", context.instance(Key.of(String.class, Hi.class)));
		assertEquals("Hello!", context.instance(Key.of(String.class, Hello.class)));
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Hi {
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	private @interface Hello {
	}

	private static class Module {
		@Provides
		@Hi
		String hi() {
			return "Hi!";
		}

		@Provides
		@Hello
		String hello() {
			return "Hello!";
		}
	}
}
