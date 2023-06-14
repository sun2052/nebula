package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ModuleOverrideTest {
	@Test
	void dependencyOverriddenByModule() {
		Context context = new Context(new PlainStubOverrideModule());
		assertEquals(PlainStub.class, context.instance(Plain.class).getClass());
	}

	@Test
	void moduleOverwrittenBySubClass() {
		assertEquals("foo", new Context(new FooModule()).instance(String.class));
		assertEquals("bar", new Context(new FooOverrideModule()).instance(String.class));
	}

	public static class Plain {
	}

	public static class PlainStub extends Plain {
	}

	public static class PlainStubOverrideModule {
		@Provides
		public Plain plain(PlainStub plainStub) {
			return plainStub;
		}
	}

	public static class FooModule {
		@Provides
		String foo() {
			return "foo";
		}
	}

	public static class FooOverrideModule extends FooModule {
		@Provides
		@Override
		String foo() {
			return "bar";
		}
	}
}
