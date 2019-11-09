package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ProviderInjectionTest {
	@Test
	void providerInjected() {
		Context context = new Context();
		assertNotNull(context.instance(A.class).plainProvider.get());
	}

	public static class A {
		private final Provider<B> plainProvider;

		@Inject
		public A(Provider<B> plainProvider) {
			this.plainProvider = plainProvider;
		}
	}

	public static class B {
	}
}
