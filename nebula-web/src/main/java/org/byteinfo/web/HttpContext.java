package org.byteinfo.web;

import org.byteinfo.util.io.IOUtil;
import org.byteinfo.util.text.RandomUtil;
import org.byteinfo.util.time.WheelTimer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class HttpContext {
	public static final AtomicLong ID_GENERATOR = new AtomicLong();
	public static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>(AppConfig.get().getInt("session.capacity"));
	public static final long SESSION_TIMEOUT = AppConfig.get().getInt("session.timeout") * 60 * 1000L;
	public static final int SESSION_ID_LENGTH = AppConfig.get().getInt("session.length");
	public static final String SESSION_COOKIE_NAME = AppConfig.get().get("session.name");
	public static final String CONTEXT_PATH = AppConfig.get().get("http.contextPath");

	private final long id;
	private final Socket socket;
	private final OutputStream out;
	private final Request request;
	private final String path;
	private String securityAttribute;
	private Map<String, Cookie> cookies;
	private Map<String, List<String>> params;
	private Session session;
	private int responseStatus = StatusCode.OK;
	private String responseType = ContentType.HTML;
	private Headers responseHeaders = new Headers();
	private Map<String, Cookie> responseCookies = new LinkedHashMap<>();
	private long responseLength;
	private boolean committed;

	private HttpContext(long id, Socket socket, OutputStream out, Request request, String path) {
		this.id = id;
		this.socket = socket;
		this.out = out;
		this.request = request;
		this.path = path;
	}

	public static HttpContext of(Socket socket, InputStream in, OutputStream out) throws IOException {
		Request request = HttpCodec.parseRequest(in);
		if (request == null) {
			return null;
		}
		return new HttpContext(ID_GENERATOR.incrementAndGet(), socket, out, request, request.path().substring(CONTEXT_PATH.length()));
	}


	/* ---------------- Request -------------- */

	public long id() {
		return id;
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
			cookies = HttpCodec.parseCookies(request.headers());
		}
		return cookies;
	}

	public Map<String, List<String>> params() throws IOException {
		if (params == null) {
			params = HttpCodec.parseParams(request);
		}
		return params;
	}

	public List<String> params(String name) throws IOException {
		return params().getOrDefault(name, Collections.emptyList());
	}

	public String param(String name) throws IOException {
		List<String> list = params().get(name);
		return list == null ? null : list.get(0);
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

	public boolean paramAsBoolean(String name) throws IOException {
		try {
			return Boolean.parseBoolean(param(name));
		} catch (Exception e) {
			throw new WebException(StatusCode.BAD_REQUEST, e);
		}
	}

	public boolean paramAsBoolean(String name, boolean defaultValue) throws IOException {
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
			Object obj = clazz.getDeclaredConstructor().newInstance();
			for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
				for (Field field : current.getDeclaredFields()) {
					Object value = param(field.getName(), field.getGenericType(), optional);
					if (value != null) {
						field.setAccessible(true);
						field.set(obj, value);
					}
				}
			}
			return (T) obj;
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
				removeCookie(SESSION_COOKIE_NAME);
			}
		}

		if (create) {
			while (true) {
				String id = RandomUtil.getAlphaNumeric(SESSION_ID_LENGTH);
				session = new Session(id);
				Session previous = SESSIONS.putIfAbsent(id, session);
				if (previous == null) {
					session.timeout = WheelTimer.getDefault().newTimeout(t -> session.destroy(), SESSION_TIMEOUT);
					Cookie c = new Cookie(SESSION_COOKIE_NAME, id);
					c.setDomain(host());
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
		return "XMLHttpRequest".equalsIgnoreCase(request.headers().get(HeaderName.REQUESTED_WITH));
	}

	public String host() {
		String host = request.headers().get(HeaderName.FORWARDED_HOST);
		if (host == null) {
			host = request.headers().get(HeaderName.HOST);
		}
		if (host != null) {
			return host.split(":", 2)[0];
		}
		return null;
	}

	public int port() {
		String port = request.headers().get(HeaderName.FORWARDED_PORT);
		if (port != null) {
			return Integer.parseInt(port);
		} else {
			return socket.getLocalPort();
		}
	}

	public String remoteAddress() {
		// X-Forwarded-For: <client>, <proxy1>, <proxy2>
		String address = request.headers().get(HeaderName.FORWARDED_FOR);
		if (address != null) {
			return address.split(",", 2)[0];
		} else {
			return ((InetSocketAddress) socket.getRemoteSocketAddress()).getAddress().getHostAddress();
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

	public HttpContext setResponseHeaders(Headers responseHeaders) {
		this.responseHeaders = responseHeaders;
		return this;
	}

	public Map<String, Cookie> responseCookies() {
		return responseCookies;
	}

	public HttpContext setResponseCookies(Map<String, Cookie> responseCookies) {
		this.responseCookies = responseCookies;
		return this;
	}

	public HttpContext removeCookie(String name) {
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

	public void redirect(String url) throws Exception {
		redirect(url, StatusCode.SEE_OTHER);
	}

	public void redirect(String url, int status) throws Exception {
		responseStatus = status;
		responseHeaders.set(HeaderName.LOCATION, url);
		commit(null);
	}

	public void download(String filename, InputStream stream) throws Exception {
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
		responseHeaders.set(HeaderName.CONTENT_DISPOSITION, String.format("attachment; filename*=%s''%s", StandardCharsets.UTF_8.name(), encoded));
		commit(stream);
	}

	public void commit(Object result) throws Exception {
		if (committed) {
			throw new IllegalStateException("Response has already been committed.");
		}

		// parse result
		InputStream in;
		if (result == null) {
			in = null;
		} else if (result instanceof Result data) {
			responseStatus = data.status();
			responseType = data.type();
			responseLength = data.length();
			in = data.stream();
			if (data.eTag() != null) {
				responseHeaders.set(HeaderName.ETAG, data.eTag());
			}
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
			throw new IllegalArgumentException("Unsupported result: " + result);
		}

		// handle conditional request
		String ifNoneMatch = headers().get(HeaderName.IF_NONE_MATCH);
		if (ifNoneMatch != null) {
			String eTag = responseHeaders.get(HeaderName.ETAG);
			if (eTag != null && (ifNoneMatch.equals(eTag) || ifNoneMatch.contains(eTag))) {
				responseStatus = StatusCode.NOT_MODIFIED;
				responseLength = 0;
				HttpCodec.send(out, responseStatus);
				committed = true;
			}
		}

		// send response
		if (!committed) {
			if (responseLength > 0) {
				responseHeaders.set(HeaderName.CONTENT_TYPE, responseType);
			}
			HttpCodec.send(out, responseStatus, responseHeaders, responseCookies.values(), in, responseLength);
			committed = true;
		}

		IOUtil.closeQuietly(in);
	}
}
