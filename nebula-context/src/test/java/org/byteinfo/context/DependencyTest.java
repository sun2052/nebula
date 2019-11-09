package org.byteinfo.context;

import org.junit.jupiter.api.Test;

import javax.inject.Provider;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DependencyTest {
	@Test
	void dependencyInstance() {
		Context context = new Context();
		assertNotNull(context.instance(Plain.class));
	}

	@Test
	void provider() {
		Context context = new Context();
		Provider<Plain> plainProvider = context.provider(Plain.class);
		assertNotNull(plainProvider.get());
	}

	@Test
	void unknown() {
		assertThrows(ContextException.class, () -> {
			Context context = new Context();
			context.instance(Unknown.class);
		});
	}

	public static class Plain {
	}

	public static class Unknown {
		public Unknown(String noSuitableConstructor) {
		}
	}
}
