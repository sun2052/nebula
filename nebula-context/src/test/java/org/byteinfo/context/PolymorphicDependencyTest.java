package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Named;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PolymorphicDependencyTest {
	@Test
	void multipleImplementations() {
		Context context = new Context(new Module());
		assertEquals(FooA.class, context.instance(Key.of(Foo.class, "A")).getClass());
		assertEquals(FooB.class, context.instance(Key.of(Foo.class, "B")).getClass());
	}

	public static class Module {
		@Provides
		@Named("A")
		Foo a(FooA fooA) {
			return fooA;
		}

		@Provides
		@Named("B")
		Foo a(FooB fooB) {
			return fooB;
		}
	}

	interface Foo {
	}

	public static class FooA implements Foo {
		@Inject
		public FooA() {
		}
	}

	public static class FooB implements Foo {
		@Inject
		public FooB() {
		}
	}
}
