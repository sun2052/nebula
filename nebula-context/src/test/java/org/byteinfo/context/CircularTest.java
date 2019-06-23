package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CircularTest {
	@Test
	void test() {
		Context context = new Context();
		assertThrows(ContextException.class, () -> context.instance(A.class));
		assertNotNull(context.instance(C.class).d);
	}

	private static class A {
		B b;

		@Inject
		public A(B b) {
			this.b = b;
		}
	}

	private static class B {
		A a;

		@Inject
		public B(A a) {
			this.a = a;
		}
	}

	private static class C {
		D d;

		@Inject
		public C(D d) {
			this.d = d;
		}
	}

	private static class D {
		Provider<C> c;

		@Inject
		public D(Provider<C> c) {
			this.c = c;
		}
	}
}
