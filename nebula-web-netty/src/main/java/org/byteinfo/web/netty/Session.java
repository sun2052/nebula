package org.byteinfo.web.netty;

import org.byteinfo.util.time.Timeout;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Session {
	private final String id;
	private final Map<String, Object> attributes = new ConcurrentHashMap<>();
	volatile Timeout timeout;

	Session(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public Object get(String name) {
		return attributes.get(name);
	}

	public Session set(String name, Object value) {
		attributes.put(name, value);
		return this;
	}

	public Session remove(String name) {
		attributes.remove(name);
		return this;
	}

	public boolean isSet(String name) {
		return attributes.containsKey(name);
	}

	public Map<String, Object> attributes() {
		return attributes;
	}

	public void destroy() {
		timeout.cancel();
		HttpContext.SESSIONS.remove(id);
	}
}
