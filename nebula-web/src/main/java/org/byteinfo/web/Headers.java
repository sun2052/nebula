package org.byteinfo.web;

import java.util.ArrayList;
import java.util.List;

public class Headers {
	private final List<Header> headers = new ArrayList<>(32);

	public int size() {
		return headers.size();
	}

	public String get(String name) {
		for (Header header : headers) {
			if (header.name().equalsIgnoreCase(name)) {
				return header.value();
			}
		}
		return null;
	}

	public void set(String name, String value) {
		remove(name);
		add(name, value);
	}

	public void add(String name, String value) {
		headers.add(new Header(name, value));
	}

	public void remove(String name) {
		headers.removeIf(header -> header.name().equalsIgnoreCase(name));
	}

	public boolean has(String name) {
		return get(name) != null;
	}

	public List<Header> values() {
		return headers;
	}
}
