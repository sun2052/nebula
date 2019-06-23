package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Singleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DependencyTest {
	@Test
	void plain() {
		Context context = new Context();
		assertEquals(context.instance(Plain.class), context.provider(Plain.class).get());
	}

	@Test
	void singleton() {
		Context context = new Context();
		assertEquals(context.instance(SingletonObj.class), context.provider(SingletonObj.class).get());
	}

	@Test
	void prototype() {
		Context context = new Context();
		assertNotEquals(context.instance(PrototypeObj.class), context.provider(PrototypeObj.class).get());
	}

	@Test
	void unknown() {
		Context context = new Context();
		assertThrows(ContextException.class, () -> context.instance(Unknown.class));
	}

	@Test
	void module() {
		assertThrows(ContextException.class, () -> new Context(new Module()));
	}

	private static class Plain {
	}

	@Singleton
	private static class SingletonObj {
	}

	@Prototype
	private static class PrototypeObj {
	}

	private static class Unknown {
		public Unknown(String unknown) {
		}
	}

	private static class Module {
		@Provides
		String hi() {
			return "hi";
		}

		@Provides
		String hello() {
			return "hello";
		}
	}
}
