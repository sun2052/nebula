package org.byteinfo.web;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import org.byteinfo.util.codec.RandomUtil;
import org.byteinfo.util.misc.Config;
import org.byteinfo.util.time.WheelTimer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Request {
	public static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>(1024);
	public static final int SESSION_TIMEOUT = Config.getInt("session.timeout");
	public static final int SESSION_ID_LENGTH = Config.getInt("session.length");
	public static final String SESSION_COOKIE_NAME = Config.get("session.name");
	public static final WheelTimer TIMER = new WheelTimer(60, SESSION_TIMEOUT);
	public static final String CONTEXT_PATH = Config.get("http.contextPath");

	private ChannelHandlerContext context;
	private FullHttpRequest request;
	private String secured;

	private QueryStringDecoder query;
	private Map<String, List<String>> params;
	private Map<String, List<FileUpload>> uploads;
	private Map<String, Cookie> cookies;

	private String path;
	private long length = Long.MIN_VALUE;

	private Map<String, Object> attributes = new HashMap<>();

	public Request(ChannelHandlerContext context, FullHttpRequest request) {
		this.context = context;
		this.request = request;
		query = new QueryStringDecoder(request.uri());
		path = CONTEXT_PATH + query.path();
	}

	public HttpMethod method() {
		return request.method();
	}

	public String path() {
		return path;
	}

	public Map<String, List<String>> params() throws IOException {
		return decodeParams();
	}

	public List<String> params(String name) throws IOException {
		return decodeParams().get(name);
	}

	public String param(String name) throws IOException {
		List<String> list = decodeParams().get(name);
		return list == null ? null : list.get(0);
	}

	public <T> T param(Class<?> clazz) {
		try {
			Object obj = clazz.getDeclaredConstructor().newInstance();
			for (Field field : clazz.getDeclaredFields()) {
				field.setAccessible(true);
				field.set(obj, Objects.requireNonNull(param(field.getName(), field.getGenericType(), true)));
			}
			return (T) obj;
		} catch (Exception e) {
			throw new WebException(HttpResponseStatus.BAD_REQUEST, e);
		}
	}

	public List<FileUpload> files(String name) throws IOException {
		decodeParams();
		return uploads.get(name);
	}

	public FileUpload file(String name) throws IOException {
		decodeParams();
		List<FileUpload> list = uploads.get(name);
		return list == null ? null : list.get(0);
	}

	public HttpHeaders headers() {
		return request.headers();
	}

	public Map<String, Cookie> cookies() {
		return decodeCookies();
	}

	public Cookie cookie(String name) {
		return decodeCookies().get(name);
	}

	public ByteBuf body() {
		return request.content();
	}

	public long length() {
		if (length != Long.MIN_VALUE) {
			try {
				length = Long.parseLong(request.headers().get(HttpHeaderNames.CONTENT_LENGTH));
			} catch (Exception e) {
				length = -1L;
			}
		}
		return length;
	}

	public boolean secure() {
		return context.pipeline().get("ssl") != null;
	}

	public boolean clientSecure() {
		String scheme = request.headers().get("x-forwarded-proto");
		if (scheme != null) {
			return "https".equalsIgnoreCase(scheme);
		} else {
			return secure();
		}
	}

	public String host() {
		String host = request.headers().get(HttpHeaderNames.HOST);
		if (host != null) {
			return host.split(":")[0];
		} else {
			return null;
		}
	}

	public String clientHost() {
		String host = request.headers().get("x-forwarded-host");
		if (host != null) {
			return host.split(":")[0];
		} else {
			return host();
		}
	}

	public int port() {
		if (secure()) {
			return Config.getInt("server.securePort");
		} else {
			return Config.getInt("server.port");
		}
	}

	public int clientPort() {
		String port = request.headers().get("x-forwarded-port");
		if (port != null) {
			return Integer.parseInt(port);
		} else {
			return port();
		}
	}

	public InetAddress remoteAddress() {
		InetSocketAddress remoteAddress = (InetSocketAddress) context.channel().remoteAddress();
		return remoteAddress.getAddress();
	}

	public String clientAddress() {
		String address = request.headers().get("x-forwarded-for");
		if (address != null) {
			return address.split(":")[0];
		} else {
			return remoteAddress().getHostAddress();
		}
	}

	public Session session() {
		return session(true);
	}

	public Session session(boolean create) {
		Cookie cookie = decodeCookies().get(SESSION_COOKIE_NAME);
		if (cookie != null) {
			String id = cookie.value();
			Session session = SESSIONS.get(id);
			if (session != null) {
				session.timeout.cancel();
				session.timeout = TIMER.newTimeout(t -> session.destroy(), SESSION_TIMEOUT);
				return session;
			}
		}

		if (create) {
			while (true) {
				String id = RandomUtil.getAlphaNumeric(SESSION_ID_LENGTH);
				Session session = new Session(id);
				Session previous = SESSIONS.putIfAbsent(id, session);
				session.timeout = TIMER.newTimeout(t -> session.destroy(), SESSION_TIMEOUT);
				if (previous == null) {
					return session;
				}
			}
		}

		return null;
	}

	public boolean xhr() {
		String requestedWith = request.headers().get("x-requested-with");
		return "XMLHttpRequest".equalsIgnoreCase(requestedWith);
	}

	public String protocol() {
		return context.pipeline().get("h2") == null ? request.protocolVersion().text() : "HTTP/2";
	}

	public Request set(String name, Object value) {
		attributes.put(name, value);
		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		return (T) attributes.get(name);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String name, T defaultValue) {
		return (T) attributes.getOrDefault(name, defaultValue);
	}

	@SuppressWarnings("unchecked")
	public <T> T remove(String name) {
		return (T) attributes.remove(name);
	}

	public Map<String, Object> attributes() {
		return attributes;
	}

	public String secured() {
		return secured;
	}

	void setSecured(String secured) {
		this.secured = secured;
	}

	private Map<String, List<String>> decodeParams() throws IOException {
		if (params == null) {
			params = new HashMap<>(query.parameters());
			uploads = new HashMap<>();
			String contentType = request.headers().get(HttpHeaderNames.CONTENT_TYPE).toLowerCase();
			if (HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.toString().equals(contentType) || HttpHeaderValues.MULTIPART_FORM_DATA.toString().equals(contentType)) {
				HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
				try {
					while (decoder.hasNext()) {
						HttpData field = (HttpData) decoder.next();
						String name = field.getName();
						if (field.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
							uploads.computeIfAbsent(name, k -> new ArrayList<>()).add((FileUpload) field);
						} else {
							params.computeIfAbsent(name, k -> new ArrayList<>()).add(field.getString());
						}
					}
				} catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
					// ignore
				} finally {
					decoder.destroy();
				}
			}
		}
		return params;
	}

	private Map<String, Cookie> decodeCookies() {
		if (cookies == null) {
			Map<String, Cookie> map = new LinkedHashMap<>();
			String cookieString = request.headers().get(HttpHeaderNames.COOKIE);
			if (cookieString != null) {
				for (Cookie cookie : ServerCookieDecoder.STRICT.decode(cookieString)) {
					map.put(cookie.name(), cookie);
				}
			}
			cookies = map;
		}
		return cookies;
	}

	private Object param(String name, Type targetType, boolean required) throws IOException {
		if (targetType instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) targetType;
			Class<?> rType = (Class<?>) pType.getRawType();
			if (rType == Optional.class) {
				return Optional.ofNullable(param(name, pType.getActualTypeArguments()[0], false));
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
				} else if (type == FileUpload.class) {
					return files(name);
				}
			}
		} else {
			Class<?> type = (Class<?>) targetType;
			String value = param(name);
			if (value == null) {
				if (required) {
					throw new IllegalArgumentException("Required argument is absent: " + name);
				} else {
					return null;
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
				} else if (type == FileUpload.class) {
					return file(name);
				}
			}
		}
		throw new IllegalArgumentException("Unsupported type: " + targetType);
	}
}
