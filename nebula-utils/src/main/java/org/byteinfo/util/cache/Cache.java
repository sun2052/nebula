package org.byteinfo.util.cache;

import java.util.function.Function;

public interface Cache<K, V> {
	V get(K key);

	default V get(K key, V defaultValue) {
		V value = get(key);
		return value == null ? defaultValue : value;
	}

	default V get(K key, Function<K, V> defaultValue) {
		V value = get(key);
		return value == null ? defaultValue.apply(key) : value;
	}

	V set(K key, V value);

	V set(K key, V value, int seconds);

	void expire(K key, int seconds);

	V remove(K key);

	int size();

	void clear();
}
