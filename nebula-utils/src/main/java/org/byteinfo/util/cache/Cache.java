package org.byteinfo.util.cache;

import org.byteinfo.util.time.Timeout;
import org.byteinfo.util.time.WheelTimer;

import java.io.Closeable;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Cache<K, V> implements Closeable {
	private final Map<K, SoftValue<V, K>> map;
	private final ReferenceQueue<V> queue;
	private final WheelTimer timer;
	private volatile boolean started;

	public Cache() {
		this(1024);
	}

	public Cache(int initialCapacity) {
		map = new ConcurrentHashMap<>(initialCapacity);
		queue = new ReferenceQueue<>();
		timer = new WheelTimer();
		started = true;

		Thread cleaner = new Thread(() -> {
			while (started) {
				try {
					SoftValue<V, K> value = (SoftValue<V, K>) queue.remove();
					map.remove(value.key);
					if (value.timeout != null) {
						value.timeout.cancel();
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		});
		cleaner.setDaemon(true);
		cleaner.start();
	}

	public V get(K key) {
		return map.get(key).get();
	}

	public void set(K key, V value) {
		map.put(key, new SoftValue<>(value, queue, key));
	}

	public void set(K key, V value, int seconds) {
		Timeout timeout = timer.newTimeout(t -> map.remove(key), seconds);
		map.put(key, new SoftValue<>(value, queue, key, timeout));
	}

	public void expire(K key, int seconds) {
		SoftValue<V, K> sv = map.get(key);
		if (sv != null) {
			sv.timeout = timer.newTimeout(t -> map.remove(key), seconds);
		}
	}

	public void remove(K key) {
		SoftValue<V, K> sv = map.get(key);
		if (sv != null) {
			map.remove(key);
			sv.timeout.cancel();
			sv.timeout = null;
		}
	}

	public void clear() {
		for (Map.Entry<K, SoftValue<V, K>> entry : map.entrySet()) {
			map.remove(entry.getKey());
			entry.getValue().timeout.cancel();
		}
	}

	public Set<Map.Entry<K, V>> entrySet() {
		Set<Map.Entry<K, SoftValue<V, K>>> entries = map.entrySet();
		if (entries.isEmpty()) {
			return Collections.emptySet();
		} else {
			Map<K, V> result = new HashMap<>(entries.size());
			for (Map.Entry<K, SoftValue<V, K>> entry : entries) {
				result.put(entry.getKey(), entry.getValue().get());
			}
			return result.entrySet();
		}
	}

	public int size() {
		return map.size();
	}

	@Override
	public void close() throws IOException {
		started = false;
	}

	private static class SoftValue<V, K> extends SoftReference<V> {
		private K key;
		private volatile Timeout timeout;

		SoftValue(V value, ReferenceQueue<V> queue, K key) {
			this(value, queue, key, null);
		}

		SoftValue(V value, ReferenceQueue<V> queue, K key, Timeout timeout) {
			super(value, queue);
			this.key = key;
			this.timeout = timeout;
		}
	}
}
