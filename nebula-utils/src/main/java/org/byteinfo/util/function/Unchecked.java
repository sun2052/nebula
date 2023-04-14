package org.byteinfo.util.function;

import java.util.Comparator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface Unchecked {
	static <T> Consumer<T> consumer(CheckedConsumer<T> consumer) {
		return t -> {
			try {
				consumer.accept(t);
			} catch (Exception e) {
				sneakyThrow(e);
			}
		};
	}

	static <T, U> BiConsumer<T, U> biConsumer(CheckedBiConsumer<T, U> biConsumer) {
		return (t, u) -> {
			try {
				biConsumer.accept(t, u);
			} catch (Exception e) {
				sneakyThrow(e);
			}
		};
	}

	static <T, R> Function<T, R> function(CheckedFunction<T, R> function) {
		return t -> {
			try {
				return function.apply(t);
			} catch (Exception e) {
				sneakyThrow(e);
				return null;
			}
		};
	}

	static <T, U, R> BiFunction<T, U, R> biFunction(CheckedBiFunction<T, U, R> biFunction) {
		return (t, u) -> {
			try {
				return biFunction.apply(t, u);
			} catch (Exception e) {
				sneakyThrow(e);
				return null;
			}
		};
	}

	static <T> Predicate<T> predicate(CheckedPredicate<T> predicate) {
		return t -> {
			try {
				return predicate.test(t);
			} catch (Exception e) {
				sneakyThrow(e);
				return false;
			}
		};
	}

	static <T, U> BiPredicate<T, U> biPredicate(CheckedBiPredicate<T, U> biPredicate) {
		return (t, u) -> {
			try {
				return biPredicate.test(t, u);
			} catch (Exception e) {
				sneakyThrow(e);
				return false;
			}
		};
	}

	static <T> Supplier<T> supplier(CheckedSupplier<T> supplier) {
		return () -> {
			try {
				return supplier.get();
			} catch (Exception e) {
				sneakyThrow(e);
				return null;
			}
		};
	}

	static Runnable runnable(CheckedRunnable runnable) {
		return () -> {
			try {
				runnable.run();
			} catch (Exception e) {
				sneakyThrow(e);
			}
		};
	}

	static <T> Comparator<T> comparator(CheckedComparator<T> comparator) {
		return (o1, o2) -> {
			try {
				return comparator.compare(o1, o2);
			} catch (Exception e) {
				sneakyThrow(e);
				return 0;
			}
		};
	}

	@SuppressWarnings("unchecked")
	static <E extends Exception> void sneakyThrow(Exception e) throws E {
		throw (E) e;
	}
}