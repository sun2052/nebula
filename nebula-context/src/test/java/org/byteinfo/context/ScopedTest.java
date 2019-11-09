package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Provider;
import javax.inject.Singleton;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ScopedTest {
	@Test
	void plain() {
		Context context = new Context();
		assertEquals(context.instance(Plain.class), context.instance(Plain.class));
	}

	@Test
	void singleton() {
		Context context = new Context();
		assertEquals(context.instance(SingletonObj.class), context.instance(SingletonObj.class));
	}

	@Test
	void singletonThroughProvider() {
		Context context = new Context();
		Provider<SingletonObj> provider = context.provider(SingletonObj.class);
		assertEquals(provider.get(), provider.get());
	}

	@Test
	void prototype() {
		Context context = new Context();
		assertNotEquals(context.instance(PrototypeObj.class), context.instance(PrototypeObj.class));
	}

	@Test
	void prototypeThroughProvider() {
		Context context = new Context();
		Provider<PrototypeObj> provider = context.provider(PrototypeObj.class);
		assertNotEquals(provider.get(), provider.get());
	}

	public static class Plain {
	}

	@Singleton
	public static class SingletonObj {
	}

	@Prototype
	public static class PrototypeObj {
	}
}
