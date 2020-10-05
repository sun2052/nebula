package org.byteinfo.web;

import java.util.HashMap;
import java.util.Map;

// https://www.iana.org/assignments/media-types/media-types.xhtml
public class ContentType {
	public static final String HTML = "text/html; charset=utf-8";
	public static final String CSS = "text/css; charset=utf-8";
	public static final String JS = "application/javascript; charset=utf-8";
	public static final String JSON = "application/json; charset=utf-8";
	public static final String TEXT = "text/plain; charset=utf-8";
	public static final String BINARY = "application/octet-stream";
	public static final String FORM = "application/x-www-form-urlencoded";
	public static final String MULTIPART = "multipart/form-data";

	private static final Map<String, String> TYPES = new HashMap<>();

	static {
		// Web Contents
		TYPES.put("html", "text/html");
		TYPES.put("css", "text/css");
		TYPES.put("js", "application/javascript");

		// Data Interchange
		TYPES.put("txt", "text/plain");
		TYPES.put("json", "application/json");
		TYPES.put("xml", "application/xml");
		TYPES.put("bin", "application/octet-stream");

		// Media Files
		TYPES.put("bmp", "image/bmp");
		TYPES.put("gif", "image/gif");
		TYPES.put("jpg", "image/jpeg");
		TYPES.put("jpeg", "image/jpeg");
		TYPES.put("png", "image/png");
		TYPES.put("svg", "image/svg+xml");
		TYPES.put("ico", "image/x-icon");

		// Web Fonts
		TYPES.put("eot", "application/vnd.ms-fontobject");
		TYPES.put("otf", "font/otf");
		TYPES.put("ttc", "font/collection");
		TYPES.put("ttf", "font/ttf");
		TYPES.put("woff", "font/woff");
		TYPES.put("woff2", "font/woff2");
	}

	public static String byName(String name) {
		int index = name.lastIndexOf('.');
		if (index != -1) {
			String ext = name.substring(index + 1);
			return TYPES.get(ext);
		}
		return BINARY;
	}
}
