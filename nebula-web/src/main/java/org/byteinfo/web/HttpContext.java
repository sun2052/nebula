package org.byteinfo.web;

import org.byteinfo.util.function.Unchecked;
import org.byteinfo.util.reflect.Reflect;
import org.byteinfo.util.text.RandomUtil;
import org.byteinfo.util.time.WheelTimer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HttpContext {
	public static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>(AppConfig.get().getInt("session.capacity"));
	public static final int SESSION_TIMEOUT = AppConfig.get().getInt("session.timeout") * 60 * 1000;
	public static final int SESSION_ID_LENGTH = AppConfig.get().getInt("session.length");
	public static final String SESSION_COOKIE_NAME = AppConfig.get().get("session.name");
	public static final String CONTEXT_PATH = AppConfig.get().get("http.contextPath");

	private final String id;
	private final Socket socket;
	private final OutputStream out;

	// request
	private final Request request;
	private final String path;
	private String securityAttribute;
	private Map<String, Cookie> cookies;
	private Map<String, List<String>> params;
	private Map<String, List<Upload>> uploads;
	private Session session;

	// response
	private int responseStatus = StatusCode.OK;
	private String responseType = ContentType.HTML;
	private final Headers responseHeaders = new Headers();
	private final Map<String, Cookie> responseCookies = new LinkedHashMap<>();
	private long responseLength = -1;
	private ResponseStream responseStream;
	private Writer responseWriter;
	private boolean headersSent;
	private boolean committed;

	public HttpContext(String id, Socket socket, OutputStream out, Request request) {
		this.id = id;
		this.socket = socket;
		this.out = out;
		this.request = request;
		this.path = request.path().substring(CONTEXT_PATH.length());
	}


	/* ---------------- Request -------------- */

	public String id() {
		return id;
	}

	public Socket socket() {
		return socket;
	}

	public String method() {
		return request.method();
	}

	public String path() {
		return path;
	}

	public String rawPath() {
		return request.path();
	}

	public String contextPath() {
		return CONTEXT_PATH;
	}

	public String securityAttribute() {
		return securityAttribute;
	}

	public Headers headers() {
		return request.headers();
	}

	public Map<String, Cookie> cookies() {
		if (cookies == null) {
			cookies = HttpCodec.parseCookies(headers());
		}
		return cookies;
	}

	public Map<String, List<String>> params() throws IOException {
		if (params == null) {
			uploads = new HashMap<>();
			params = HttpCodec.parseParams(request, uploads);
		}
		return params;
	}

	public List<String> params(String name) throws IOException {
		return params().getOrDefault(name, List.of());
	}

	public String param(String name) throws IOException {
		List<String> list = params().get(name);
		return list == null ? null : list.getFirst();
	}

	public String param(String name, String defaultValue) throws IOException {
		String value = param(name);
		return value == null ? defaultValue : value;
	}

	public int paramAsInt(String name) {
		try {
			return Integer.parseInt(param(name));
		} catch (Exception e) {
			throw new WebException(StatusCode.BAD_REQUEST, e);
		}
	}

	public int paramAsInt(String name, int defaultValue) {
		try {
			return Integer.parseInt(param(name));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public long paramAsLong(String name) {
		try {
			return Long.parseLong(param(name));
		} catch (Exception e) {
			throw new WebException(StatusCode.BAD_REQUEST, e);
		}
	}

	public long paramAsLong(String name, long defaultValue) {
		try {
			return Long.parseLong(param(name));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public boolean paramAsBoolean(String name) {
		try {
			return Boolean.parseBoolean(param(name));
		} catch (Exception e) {
			throw new WebException(StatusCode.BAD_REQUEST, e);
		}
	}

	public boolean paramAsBoolean(String name, boolean defaultValue) {
		try {
			return Boolean.parseBoolean(param(name));
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public <T> T param(Class<T> clazz) {
		return param(clazz, false);
	}

	public <T> T ifParam(Class<T> clazz) {
		return param(clazz, true);
	}

	public <T> T param(Class<T> clazz, boolean optional) {
		try {
			return Reflect.create(clazz, Unchecked.biFunction((name, type) -> param(name, type, optional)));
		} catch (Exception e) {
			throw new WebException(StatusCode.BAD_REQUEST, e);
		}
	}

	public Object param(String name, Type targetType, boolean optional) throws IOException {
		if (targetType instanceof ParameterizedType pType) {
			Class<?> rType = (Class<?>) pType.getRawType();
			if (rType == Optional.class) {
				return Optional.ofNullable(param(name, pType.getActualTypeArguments()[0], true));
			} else if (rType == List.class) {
				Class<?> type = (Class<?>) pType.getActualTypeArguments()[0];
				if (type == String.class) {
					return params(name);
				} else if (type == int.class || type == Integer.class) {
					return params(name).stream().map(Integer::parseInt).collect(Collectors.toList());
				} else if (type == long.class || type == Long.class) {
					return params(name).stream().map(Long::parseLong).collect(Collectors.toList());
				} else if (type == double.class || type == Double.class) {
					return params(name).stream().map(Double::parseDouble).collect(Collectors.toList());
				}
			}
		} else {
			Class<?> type = (Class<?>) targetType;
			String value = param(name);
			if (value == null) {
				if (optional) {
					return null;
				} else {
					throw new IllegalArgumentException("Required argument is absent: " + name);
				}
			} else {
				if (type == String.class) {
					return value;
				} else if (type == int.class || type == Integer.class) {
					return Integer.parseInt(value);
				} else if (type == long.class || type == Long.class) {
					return Long.parseLong(value);
				} else if (type == double.class || type == Double.class) {
					return Double.parseDouble(value);
				} else if (type == boolean.class || type == Boolean.class) {
					return Boolean.parseBoolean(value);
				}
			}
		}
		throw new IllegalArgumentException("Unsupported type: " + targetType);
	}

	public Map<String, List<Upload>> files() throws IOException {
		params();
		return uploads;
	}

	public List<Upload> files(String name) throws IOException {
		return files().getOrDefault(name, List.of());
	}

	public Upload file(String name) throws IOException {
		List<Upload> list = files().get(name);
		return list == null ? null : list.getFirst();
	}

	public InputStream body() {
		return request.body();
	}

	public long length() {
		return request.length();
	}

	public Session session(boolean create) {
		if (session != null) {
			return session;
		}

		Cookie cookie = cookies().get(SESSION_COOKIE_NAME);
		if (cookie != null) {
			String id = cookie.getValue();
			session = SESSIONS.get(id);
			if (session != null) {
				session.timeout.cancel();
				session.timeout = WheelTimer.getDefault().newTimeout(t -> session.destroy(), SESSION_TIMEOUT);
				return session;
			} else {
				removeResponseCookie(SESSION_COOKIE_NAME);
			}
		}

		if (create) {
			while (true) {
				String id = RandomUtil.randomAlphaNumeric(SESSION_ID_LENGTH);
				session = new Session(id);
				Session previous = SESSIONS.putIfAbsent(id, session);
				if (previous == null) {
					session.timeout = WheelTimer.getDefault().newTimeout(t -> session.destroy(), SESSION_TIMEOUT);
					Cookie c = new Cookie(SESSION_COOKIE_NAME, id);
					c.setDomain(domain());
					c.setHttpOnly(true);
					responseCookies.put(SESSION_COOKIE_NAME, c);
					return session;
				}
			}
		}

		return null;
	}

	public Session session() {
		return session(true);
	}

	public Session ifSession() {
		return session(false);
	}

	public boolean xhr() {
		return HeaderValue.XML_HTTP_REQUEST.equals(headers().get(HeaderName.REQUESTED_WITH));
	}

	public String host() {
		String host = headers().get(HeaderName.FORWARDED_HOST);
		if (host == null) {
			host = headers().get(HeaderName.HOST);
		}
		return host;
	}

	public int port() {
		String port = headers().get(HeaderName.FORWARDED_PORT);
		if (port != null) {
			return Integer.parseInt(port);
		} else {
			return socket.getLocalPort();
		}
	}

	public String domain() {
		return host().split(":", 2)[0];
	}

	public String remoteAddress() {
		// X-Forwarded-For: <client>, <proxy1>, <proxy2>
		String address = headers().get(HeaderName.FORWARDED_FOR);
		if (address != null) {
			return address.split(",", 2)[0];
		} else {
			return socket.getInetAddress().getHostAddress();
		}
	}

	void setSecurityAttribute(String attribute) {
		securityAttribute = attribute;
	}


	/* ---------------- Response -------------- */

	public boolean isCommitted() {
		return committed;
	}

	public int responseStatus() {
		return responseStatus;
	}

	public HttpContext setResponseStatus(int status) {
		this.responseStatus = status;
		return this;
	}

	public String responseType() {
		return responseType;
	}

	public HttpContext setResponseType(String responseType) {
		this.responseType = responseType;
		return this;
	}

	public Headers responseHeaders() {
		return responseHeaders;
	}

	public Map<String, Cookie> responseCookies() {
		return responseCookies;
	}

	public HttpContext removeResponseCookie(String name) {
		Cookie cookie = new Cookie(name, "");
		cookie.setMaxAge(0);
		responseCookies.put(name, cookie);
		return this;
	}

	public long responseLength() {
		return responseLength;
	}

	public void setResponseLength(long responseLength) {
		this.responseLength = responseLength;
	}

	public OutputStream responseStream() {
		if (responseStream == null) {
			responseStream = new ResponseStream(out, this);
		}
		return responseStream;
	}

	public Writer responseWriter() {
		if (responseWriter == null) {
			responseWriter = new OutputStreamWriter(responseStream());
		}
		return responseWriter;
	}

	public void redirect(String url) throws Exception {
		redirect(url, StatusCode.SEE_OTHER);
	}

	public void redirect(String url, int status) throws Exception {
		responseStatus = status;
		responseHeaders.set(HeaderName.LOCATION, url);
		commit(null);
	}

	public void download(String filename, InputStream stream) throws Exception {
		responseType = ContentType.BINARY;
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
		responseHeaders.set(HeaderName.CONTENT_DISPOSITION, String.format("attachment; filename*=%s''%s", StandardCharsets.UTF_8.name(), encoded));
		if (stream != null) {
			commit(stream);
		}
	}

	public void sendHeaders() throws IOException {
		ensureNotCommitted();
		ensureHeadersNotSent();
		headersSent = true;
		HttpCodec.send(out, responseStatus, responseHeaders, responseCookies.values(), responseType, responseLength, null);
	}

	public void commit(Object result) throws IOException {
		ensureNotCommitted();
		committed = true;

		if (headersSent) {
			if (responseWriter != null) {
				responseWriter.close();
			} else if (responseStream != null) {
				responseStream.close();
			}
			return;
		}

		// parse result
		InputStream in = null;
		if (result == null) {
			responseLength = 0;
		} else if (result instanceof Result data) {
			responseStatus = data.status();
			responseType = data.type();
			responseLength = data.length();
			if (data.eTag() != null) {
				responseHeaders.set(HeaderName.ETAG, data.eTag());
			}
			in = data.stream();
		} else if (result instanceof String data) {
			byte[] buffer = data.getBytes();
			responseLength = buffer.length;
			in = new ByteArrayInputStream(buffer);
		} else if (result instanceof byte[] data) {
			responseLength = data.length;
			in = new ByteArrayInputStream(data);
		} else if (result instanceof InputStream data) {
			in = data;
		} else {
			throw new IllegalArgumentException("Unsupported result type: " + result);
		}

		try (var ignored = in) {
			boolean bodySent = false;

			// handle conditional request
			String ifNoneMatch = headers().get(HeaderName.IF_NONE_MATCH);
			if (ifNoneMatch != null) {
				String eTag = responseHeaders.get(HeaderName.ETAG);
				if (eTag != null && (ifNoneMatch.equals(eTag) || ifNoneMatch.contains(eTag))) {
					bodySent = true;
					HttpCodec.send(out, StatusCode.NOT_MODIFIED);
				}
			}

			// send full response
			if (!bodySent) {
				HttpCodec.send(out, responseStatus, responseHeaders, responseCookies.values(), responseType, responseLength, in);
			}
		}
	}

	private void ensureHeadersNotSent() {
		if (headersSent) {
			throw new IllegalStateException("Headers has already been sent.");
		}
	}

	private void ensureNotCommitted() {
		if (committed) {
			throw new IllegalStateException("Response has already been committed.");
		}
	}
}
