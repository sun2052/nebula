package org.byteinfo.web;

import org.byteinfo.util.io.LimitedInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP/1.1 Codec
 *
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110">RFC 9110: HTTP Semantics</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc9112">RFC 9112: HTTP/1.1</a>
 * @see <a href="https://www.rfc-editor.org/rfc/rfc7578">RFC 7578: Returning Values from Forms: multipart/form-data</a>
 * @see <a href="https://html.spec.whatwg.org/multipage/">HTML Standard</a>
 */
public interface HttpCodec {
	byte[] CRLF = {'\r', '\n'};
	byte[] VERSION = "HTTP/1.1 ".getBytes();

	/**
	 * Parses the HTTP request.
	 *
	 * @param in input stream of the request
	 * @return http request
	 * @throws IOException if an io error occurs
	 * @throws EOFException if the connection is closed
	 * @throws WebException if the request can't be parsed
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc9112#name-message-format">Message Format</a>
	 */
	static Request parseRequest(InputStream in) throws IOException {
		RequestLine request = readRequestLine(in);
		Headers headers = readHeaders(in);
		long length = -1;
		InputStream body;
		if (HeaderValue.CHUNKED.equals(headers.get(HeaderName.TRANSFER_ENCODING))) {
			body = new ChunkedInputStream(in);
		} else {
			String header = headers.get(HeaderName.CONTENT_LENGTH);
			length = header == null ? 0 : Long.parseLong(header);
			body = new LimitedInputStream(in, length);
		}
		return new Request(request.method(), request.path(), request.query(), headers, length, body);
	}

	/**
	 * Reads the request line.
	 *
	 * @param in input stream of the request
	 * @return request line
	 * @throws IOException if an io error occurs
	 * @throws EOFException if the connection is closed
	 * @throws WebException if the request can't be parsed
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc9112#name-request-line">Request Line</a>
	 */
	static RequestLine readRequestLine(InputStream in) throws IOException {
		String line = readLine(in, true);
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
		return new RequestLine(method, path, query);
	}

	/**
	 * Reads the request headers.
	 *
	 * @param in input stream of the request
	 * @return request headers
	 * @throws IOException if an io error occurs
	 * @throws EOFException if the connection is closed
	 * @throws WebException if the request can't be parsed
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc9112#name-field-syntax">Field Syntax</a>
	 */
	static Headers readHeaders(InputStream in) throws IOException {
		Headers headers = new Headers();
		String line;
		while (!(line = readLine(in, false)).isEmpty()) {
			String[] parts = line.split(":", 2);
			if (parts.length != 2) {
				throw new WebException(StatusCode.BAD_REQUEST, "invalid request header: " + line);
			}
			headers.add(parts[0], parts[1].trim());
		}
		return headers;
	}

	/**
	 * Parse the header parameters.
	 *
	 * @param value header value
	 * @return params map
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc9110#name-parameters">Parameters</a>
	 */
	static Map<String, String> parseHeaderParams(String value) {
		if (value == null) {
			return Map.of();
		}
		Map<String, String> params = new LinkedHashMap<>();
		for (String part : value.split(";")) {
			String[] pair = part.trim().split("=", 2);
			String val = "";
			if (pair.length == 2) {
				val = pair[1];
				if (val.charAt(0) == '"') { // trim possible quotes
					val = val.substring(1, val.length() - 1);
				}
			}
			params.put(pair[0], val);
		}
		return params;
	}

	/**
	 * Parses the request cookies.
	 *
	 * @param headers request headers
	 * @return a map of cookie name and corresponding cookie
	 * @see <a href="https://www.rfc-editor.org/rfc/rfc6265#section-4.2">Syntax</a>
	 */
	static Map<String, Cookie> parseCookies(Headers headers) {
		Map<String, String> params = parseHeaderParams(headers.get(HeaderName.COOKIE));
		Map<String, Cookie> cookies = new LinkedHashMap<>();
		for (Map.Entry<String, String> entry : params.entrySet()) {
			if (!entry.getValue().isEmpty()) {
				cookies.put(entry.getKey(), new Cookie(entry.getKey(), entry.getValue()));
			}
		}
		return cookies;
	}

	/**
	 * Parses all request params from query string, urlencoded form and multipart form.
	 *
	 * @param request the HTTP request
	 * @param uploads parsed uploads if is multipart request
	 * @return a map of param name and corresponding param value list
	 * @throws IOException if an io error occurs
	 * @throws WebException if the request can't be parsed
	 * @see <a href="https://html.spec.whatwg.org/multipage/form-control-infrastructure.html#form-submission-2">Form submission</a>
	 */
	static Map<String, List<String>> parseParams(Request request, Map<String, List<Upload>> uploads) throws IOException {
		Map<String, String> headerParams = parseHeaderParams(request.headers().get(HeaderName.CONTENT_TYPE));
		List<String> urlencodedList = new ArrayList<>();
		urlencodedList.add(request.query());
		if (headerParams.containsKey(ContentType.FORM)) {
			urlencodedList.add(new String(request.body().readAllBytes()));
		}

		// parse query string and urlencoded form
		Map<String, List<String>> params = new HashMap<>();
		for (String urlencoded : urlencodedList) {
			if (urlencoded == null || urlencoded.isEmpty()) {
				continue;
			}
			for (String part : urlencoded.split("&")) {
				try {
					String[] pair = part.split("=", 2);
					List<String> values = params.computeIfAbsent(URLDecoder.decode(pair[0], StandardCharsets.UTF_8), k -> new ArrayList<>());
					if (pair.length == 1) {
						values.add("");
					} else if (pair.length == 2) {
						values.add(URLDecoder.decode(pair[1], StandardCharsets.UTF_8));
					}
				} catch (Exception e) {
					throw new WebException(StatusCode.BAD_REQUEST, "invalid param: " + part, e);
				}
			}
		}

		// parse multipart form
		if (headerParams.containsKey(ContentType.MULTIPART)) {
			byte[] boundary = ("\r\n" + "--" + headerParams.get("boundary") + "\r\n").getBytes(); // CRLF--boundaryCRLF
			int length = (int) (request.length() - (boundary.length + 2)); // without last boundary: CRLF--boundary--CRLF
			byte[] body = request.body().readNBytes(length);
			if (body.length != length) {
				throw new IOException("unexpected end of stream");
			}
			request.body().transferTo(OutputStream.nullOutputStream()); // discard last boundary
			int offset = boundary.length - 2; // first boundary: --boundaryCRLF
			while (offset < body.length) {
				int nextOffset = findBoundary(body, boundary, offset);
				try (var in = new ByteArrayInputStream(Arrays.copyOfRange(body, offset, nextOffset))) {
					Headers headers = readHeaders(in);
					byte[] data = in.readAllBytes();
					Map<String, String> map = parseHeaderParams(headers.get(HeaderName.CONTENT_DISPOSITION));
					String name = map.get("name");
					String originalName = map.get("filename");
					if (originalName == null) { // text
						params.computeIfAbsent(name, k -> new ArrayList<>()).add(new String(data));
					} else { // upload
						Upload upload = new Upload(name, originalName, headers.get(HeaderName.CONTENT_TYPE), data);
						uploads.computeIfAbsent(name, k -> new ArrayList<>()).add(upload);
					}
				}
				offset = nextOffset + boundary.length;
			}
		}
		return params;
	}

	private static int findBoundary(byte[] body, byte[] boundary, int offset) {
		for (int i = offset; i < body.length - boundary.length; i++) {
			int j;
			for (j = 0; j < boundary.length; j++) {
				if (body[i + j] != boundary[j]) {
					break;
				}
			}
			if (j == boundary.length) {
				return i;
			}
		}
		return body.length;
	}

	/**
	 * Reads a line of ASCII characters terminated by "\r\n".
	 *
	 * @param in data source
	 * @param ignoreEmpty ignore empty lines
	 * @return a line of ASCII characters
	 * @throws IOException if an io error occurs
	 * @throws EOFException if the connection is closed
	 */
	static String readLine(InputStream in, boolean ignoreEmpty) throws IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream(1024 * 8);
		do {
			int ch;
			while ((ch = in.read()) != -1) {
				if (ch == '\r') {
					in.read(); // discard next '\n'
					break;
				}
				buffer.write(ch);
			}
			if (ch == -1) {
				throw new EOFException();
			}
		} while (ignoreEmpty && buffer.size() == 0);
		return buffer.toString();
	}

	/**
	 * Sends a minimal HTTP response.
	 *
	 * @param out response output stream
	 * @param status response status
	 * @throws IOException if an io error occurs
	 */
	static void send(OutputStream out, int status) throws IOException {
		send(out, status, null, null, null, 0);
	}

	/**
	 * Sends a full HTTP response.
	 *
	 * @param out response output stream
	 * @param status response status
	 * @param headers response headers or null if not required
	 * @param cookies response cookies or null if not required
	 * @param data response body or null if not required
	 * @param length response body length
	 * @throws IOException if an io error occurs
	 */
	static void send(OutputStream out, int status, Headers headers, Collection<Cookie> cookies, InputStream data, long length) throws IOException {
		// write status line
		out.write(VERSION);
		out.write(String.valueOf(status).getBytes());
		out.write(CRLF);

		// prepare headers
		if (headers == null) {
			headers = new Headers();
		}
		if (length >= 0) {
			headers.set(HeaderName.CONTENT_LENGTH, String.valueOf(length));
		} else {
			headers.set(HeaderName.TRANSFER_ENCODING, HeaderValue.CHUNKED);
		}

		// encode cookies
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getPath() == null) {
					cookie.setPath("/");
				}
				headers.add(HeaderName.SET_COOKIE, cookie.toString());
			}
		}

		// write headers
		for (Header header : headers.values()) {
			out.write(header.name().getBytes());
			out.write(": ".getBytes());
			out.write(header.value().getBytes());
			out.write(CRLF);
		}
		out.write(CRLF);

		// write response body
		if (length != 0 && data != null) {
			if (length > 0) {
				data.transferTo(out);
			} else {
				try (var chunked = new ChunkedOutputStream(out)) {
					data.transferTo(chunked);
				}
			}
		}
		out.flush();
	}
}
