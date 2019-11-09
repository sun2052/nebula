package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QualifiedDependencyTest {
	@Test
	void qualifiedInstances() {
		Context context = new Context(new Module());
		assertEquals(FooA.class, context.instance(Key.of(Foo.class, A.class)).getClass());
		assertEquals(FooB.class, context.instance(Key.of(Foo.class, B.class)).getClass());
	}

	@Test
	void injectedQualified() {
		Context context = new Context(new Module());
		Dummy dummy = context.instance(Dummy.class);
		assertEquals(FooB.class, dummy.foo.getClass());
	}

	@Test
	void fieldInjectedQualified() {
		Context context = new Context(new Module());
		DummyTestUnit dummy = context.instance(DummyTestUnit.class);
		assertEquals(FooA.class, dummy.foo.getClass());
	}

	interface Foo {
	}

	public static class FooA implements Foo {
	}

	public static class FooB implements Foo {
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@interface A {
	}

	@Qualifier
	@Retention(RetentionPolicy.RUNTIME)
	@interface B {
	}

	public static class Module {
		@Provides
		@A
		Foo a(FooA fooA) {
			return fooA;
		}

		@Provides
		@B
		Foo b(FooB fooB) {
			return fooB;
		}
	}

	public static class Dummy {
		private final Foo foo;

		@Inject
		public Dummy(@B Foo foo) {
			this.foo = foo;
		}
	}

	public static class DummyTestUnit {
		@Inject
		@A
		private Foo foo;
	}
}
