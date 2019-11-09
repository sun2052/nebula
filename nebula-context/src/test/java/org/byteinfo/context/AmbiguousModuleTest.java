package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AmbiguousModuleTest {
	@Test
	void ambiguousModule() {
		assertThrows(ContextException.class, () -> new Context(new Module()));
	}

	public static class Module {
		@Provides
		String foo() {
			return "foo";
		}

		@Provides
		String bar() {
			return "bar";
		}
	}
}
