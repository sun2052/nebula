package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransitiveFieldsDependencyTest {
	@Test
	void transitiveFields() {
		Context context = new Context();
		A a = context.instance(A.class);
		assertNotNull(a.b.c);
		assertTrue(a.b.c.postConstructRan);
	}

	public static class A {
		@Inject
		private B b;
	}

	public static class B {
		@Inject
		private C c;
	}

	public static class C {
		boolean postConstructRan;

		@PostConstruct
		void postConstruct() {
			postConstructRan = true;
		}
	}
}
