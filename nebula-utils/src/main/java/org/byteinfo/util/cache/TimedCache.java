package org.byteinfo.util.cache;

import org.byteinfo.util.time.Timeout;
import org.byteinfo.util.time.WheelTimer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class TimedCache<K, V> implements Cache<K, V> {
	private final Map<K, V> map;
	private final Function<K, V> loader;
	private final Map<K, Timeout> timeoutMap = new ConcurrentHashMap<>();

	public TimedCache() {
		this(1024);
	}

	public TimedCache(int initialCapacity) {
		this(initialCapacity, k -> null);
	}

	public TimedCache(int initialCapacity, Function<K, V> loader) {
		this.map = new ConcurrentHashMap<>(initialCapacity);
		this.loader = loader;
	}

	@Override
	public V get(K key) {
		return map.computeIfAbsent(key, loader);
	}

	@Override
	public V set(K key, V value) {
		timeoutMap.remove(key);
		return map.put(key, value);
	}

	@Override
	public V set(K key, V value, int seconds) {
		timeoutMap.put(key, WheelTimer.getDefault().newTimeout(t -> remove(key), seconds));
		return map.put(key, value);
	}

	@Override
	public void expire(K key, int seconds) {
		Timeout timeout = timeoutMap.put(key, WheelTimer.getDefault().newTimeout(t -> remove(key), seconds));
		if (timeout != null) {
			timeout.cancel();
		}
	}

	@Override
	public V remove(K key) {
		timeoutMap.remove(key);
		return map.remove(key);
	}

	@Override
	public int size() {
		return map.size();
	}

	@Override
	public void clear() {
		timeoutMap.clear();
		map.clear();
	}
}
