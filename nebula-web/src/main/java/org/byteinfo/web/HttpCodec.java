package org.byteinfo.web;

import org.byteinfo.util.io.LimitedInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public interface HttpCodec {
	byte[] CRLF = {'\r', '\n'};
	byte[] VERSION = "HTTP/1.1 ".getBytes(StandardCharsets.UTF_8);

	static Request parseRequest(InputStream in) throws IOException {
		String line;
		while ((line = readLine(in)).length() == 0) {
			// ignore empty lines
		}

		// parse request line
		String[] parts = line.split(" ");
		if (parts.length != 3) {
			throw new WebException(StatusCode.BAD_REQUEST, "invalid request line: " + line);
		}
		String method = parts[0];
		String path = parts[1];
		String query = null;
		int index = path.indexOf('?');
		if (index != -1) {
			query = path.substring(index + 1);
			path = path.substring(0, index);
		}

		// parse headers
		Headers headers = new Headers();
		while ((line = readLine(in)).length() > 0) {
			int separator = line.indexOf(':');
			if (separator < 0) {
				throw new WebException(StatusCode.BAD_REQUEST, "invalid header: " + line);
			}
			headers.add(line.substring(0, separator), line.substring(separator + 1).trim());
		}

		// parse request body
		String header = headers.get(HeaderName.CONTENT_LENGTH);
		long length = header == null ? 0 : Long.parseLong(header);
		return new Request(method, path, query, headers, length, new LimitedInputStream(in, length));
	}

	static Map<String, Cookie> decodeCookies(Headers headers) {
		Map<String, Cookie> map = new LinkedHashMap<>();
		String cookieString = headers.get(HeaderName.COOKIE);
		if (cookieString != null) {
			for (String cookie : cookieString.split("; ")) {
				String[] pair = cookie.split("=");
				if (pair.length != 2) {
					throw new WebException(StatusCode.BAD_REQUEST, "invalid cookie: " + cookie);
				}
				map.put(pair[0], new Cookie(pair[0], pair[1]));
			}
		}
		return map;
	}

	static Map<String, List<String>> decodeParams(Request request) throws IOException {
		Map<String, List<String>> params = new HashMap<>();
		List<String> dataList = new ArrayList<>();
		dataList.add(request.query());
		String contentType = request.headers().get(HeaderName.CONTENT_TYPE);
		if (contentType != null) {
			int index = contentType.indexOf(";");
			if (index != -1) {
				contentType = contentType.substring(0, index);
			}
		}
		if (ContentType.FORM.equalsIgnoreCase(contentType)) {
			dataList.add(new String(request.body().readAllBytes(), StandardCharsets.UTF_8));
		}
		for (String data : dataList) {
			if (data == null || data.length() == 0) {
				continue;
			}
			for (String part : data.split("&")) {
				String[] pair = part.split("=");
				List<String> values = params.computeIfAbsent(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), k -> new ArrayList<>());
				if (pair.length == 1) {
					values.add("");
				} else if (pair.length == 2) {
					values.add(URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
				} else {
					throw new WebException(StatusCode.BAD_REQUEST, "invalid param: " + part);
				}
			}
		}
		return params;
	}

	static void writeResponse(OutputStream out, int status, Headers headers, Collection<Cookie> cookies, InputStream in, long length) throws IOException {
		out.write(VERSION);
		out.write(String.valueOf(status).getBytes(StandardCharsets.UTF_8));
		out.write(CRLF);

		if (headers == null) {
			headers = new Headers();
		}

		if (in != null) {
			headers.set(HeaderName.CONTENT_LENGTH, String.valueOf(length));
		}

		if (cookies != null) {
			for (Cookie cookie : cookies) {
				headers.add(HeaderName.SET_COOKIE, cookie.toString());
			}
		}

		for (Header header : headers.values()) {
			out.write(header.name().getBytes(StandardCharsets.UTF_8));
			out.write(": ".getBytes(StandardCharsets.UTF_8));
			out.write(header.value().getBytes(StandardCharsets.UTF_8));
			out.write(CRLF);
		}

		out.write(CRLF);

		if (in != null) {
			in.transferTo(out);
		}
		out.flush();
	}

	static void writeResponse(OutputStream out, int status, Headers headers, Collection<Cookie> cookies) throws IOException {
		writeResponse(out, status, headers, cookies, null, 0);
	}

	static void writeResponse(OutputStream out, int status, Headers headers) throws IOException {
		writeResponse(out, status, headers, null);
	}

	static void writeResponse(OutputStream out, int status) throws IOException {
		writeResponse(out, status, null);
	}

	static String readLine(InputStream in) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		int ch;
		while ((ch = in.read()) != -1) {
			if (ch == '\r') {
				in.read(); // discard next '\n'
				break;
			}
			buffer.write(ch);
		}
		return buffer.toString(StandardCharsets.UTF_8);
	}
}
