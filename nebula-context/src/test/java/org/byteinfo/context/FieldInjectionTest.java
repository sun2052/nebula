package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FieldInjectionTest {
	@Test
	void fieldsInjected() {
		Context context = new Context();
		Target target = context.instance(Target.class);
		assertNotNull(target.a);
		assertTrue(target.postConstructCalled);
	}

	public static class Target {
		@Inject
		private A a;

		private boolean postConstructCalled;

		@PostConstruct
		void postConstruct() {
			postConstructCalled = true;
		}
	}

	public static class A {
	}
}
