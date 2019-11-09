package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PojoProvidedThroughModuleTest {
	@Test
	void pojoNotProvided() {
		assertThrows(ContextException.class, () -> {
			Context context = new Context();
			context.instance(Pojo.class);
		});
	}

	@Test
	void pojoProvided() {
		Context context = new Context(new Module());
		assertNotNull(context.instance(Pojo.class));
	}

	public static class Module {
		@Provides
		Pojo pojo() {
			return new Pojo("foo");
		}
	}

	public static class Pojo {
		private final String foo;

		public Pojo(String foo) {
			this.foo = foo;
		}
	}
}
