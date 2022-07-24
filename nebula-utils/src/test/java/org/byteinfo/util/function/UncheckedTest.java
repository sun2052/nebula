package org.byteinfo.util.function;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UncheckedTest {
	@Test
	void testConsumer() {
		// ClassNotFoundException
		Stream.of("java.lang.Object").forEach(Unchecked.consumer(Class::forName));
	}

	@Test
	void testBiConsumer() {
		// ClassNotFoundException
		Map.of("java.lang.", "Object").forEach(Unchecked.biConsumer((k, v) -> Class.forName(k + v)));
	}

	@Test
	void testFunction() {
		// ClassNotFoundException
		Stream.of("java.lang.Object").map(Unchecked.function(Class::forName)).toList();
	}

	@Test
	void testBiFunction() {
		// ClassNotFoundException
		BiFunction<String, String, Object> biFunction = Unchecked.biFunction((t, u) -> Class.forName(t + u));
		assertSame(biFunction.apply("java.lang.", "Object"), Object.class);
	}

	@Test
	void testPredicate() {
		// ClassNotFoundException
		Stream.of("java.lang.Object").filter(Unchecked.predicate(t -> Class.forName(t) == Object.class)).toList();
	}

	@Test
	void testBiPredicate() {
		// ClassNotFoundException
		BiPredicate<String, String> biPredicate = Unchecked.biPredicate((t, u) -> Class.forName(t + u) == Object.class);
		assertTrue(biPredicate.test("java.lang.", "Object"));
	}

	@Test
	void testSupplier() {
		// ClassNotFoundException
		Unchecked.supplier(() -> Class.forName("java.lang.Object"));
	}

	@Test
	void testRunnable() {
		// ClassNotFoundException
		Unchecked.runnable(() -> Class.forName("java.lang.Object"));
	}

	@Test
	void testSneakyThrow() {
		// ClassNotFoundException
		assertThrows(ClassNotFoundException.class, () -> Class.forName("not.found"));
	}
}