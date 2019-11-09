package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TransitiveDependencyTest {
	@Test
	void transitive() {
		Context context = new Context();
		A a = context.instance(A.class);
		assertNotNull(a.b.c);
	}

	public static class A {
		private final B b;

		@Inject
		public A(B b) {
			this.b = b;
		}
	}

	public static class B {
		private final C c;

		@Inject
		public B(C c) {
			this.c = c;
		}
	}

	public static class C {
	}
}
