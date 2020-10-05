package org.byteinfo.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpData;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.stream.ChunkedStream;
import io.netty.util.AsciiString;
import org.byteinfo.util.codec.RandomUtil;
import org.byteinfo.util.io.IOUtil;
import org.byteinfo.util.misc.Config;
import org.byteinfo.util.time.WheelTimer;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class HttpContext {
	public static final Map<String, Session> SESSIONS = new ConcurrentHashMap<>(1024);
	public static final int SESSION_TIMEOUT = Config.getInt("session.timeout") * 60;
	public static final int SESSION_ID_LENGTH = Config.getInt("session.length");
	public static final String SESSION_COOKIE_NAME = Config.get("session.name");

	public static final String CONTEXT_PATH = Config.get("http.contextPath");
	public static final long MAX_AGE = Config.getInt("asset.maxAge");
	public static final boolean CACHE_BY_ETAG = Config.getBoolean("asset.eTag");

	private ChannelHandlerContext context;
	private FullHttpRequest request;
	private int bufferSize;
	private boolean keepAlive;
	private Map<String, Object> attributes = new ConcurrentHashMap<>();
	private String securityAttribute;

	// request
	private QueryStringDecoder query;
	private Map<String, List<String>> params;
	private Map<String, List<FileUpload>> uploads;
	private Map<String, Cookie> cookies;
	private String requestPath;
	private String realPath;
	private String genericPath;
	private Session session;

	// response
	private HttpResponseStatus responseStatus = HttpResponseStatus.OK;
	private MediaType responseType = MediaType.OCTETSTREAM;
	private HttpHeaders responseHeaders = new DefaultHttpHeaders();
	private Map<String, Cookie> responseCookies = new LinkedHashMap<>();
	private long responseLength = -1;
	private boolean committed;

	public HttpContext(ChannelHandlerContext context, FullHttpRequest request, int bufferSize) {
		this.context = context;
		this.request = request;
		this.bufferSize = bufferSize;
		this.keepAlive = HttpUtil.isKeepAlive(request);

		if (keepAlive) {
			responseHeaders.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}

		this.query = new QueryStringDecoder(request.uri());
		this.requestPath = query.path();
		this.realPath = requestPath.substring(CONTEXT_PATH.length());


		AsciiString stringIdName = HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text();
		String streamId = request.headers().get(stringIdName);
		if (streamId != null) {
			responseHeaders.set(stringIdName, streamId);
		}
	}


	/* ---------------- Request -------------- */

	public Channel channel() {
		return context.channel();
	}

	public HttpMethod method() {
		return request.method();
	}

	public String requestPath() {
		return requestPath;
	}

	public String contextPath() {
		return CONTEXT_PATH;
	}

	public String realPath() {
		return realPath;
	}

	public String genericPath() {
		return genericPath;
	}

	public Map<String, List<String>> params() throws IOException {
		return decodeParams();
	}

	public List<String> params(String name) throws IOException {
		return decodeParams().getOrDefault(name, Collections.emptyList());
	}

	public String param(String name) throws IOException {
		List<String> list = decodeParams().get(name);
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
			throw new WebException(HttpResponseStatus.BAD_REQUEST, e);
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
			throw new WebException(HttpResponseStatus.BAD_REQUEST, e);
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
			throw new WebException(HttpResponseStatus.BAD_REQUEST, e);
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
		try {
			Object obj = clazz.getDeclaredConstructor().newInstance();
			for (Class<?> current = clazz; current != Object.class; current = current.getSuperclass()) {
				for (Field field : current.getDeclaredFields()) {
					field.setAccessible(true);
					field.set(obj, Objects.requireNonNull(param(field.getName(), field.getGenericType(), true)));
				}
			}
			return (T) obj;
		} catch (Exception e) {
			throw new WebException(HttpResponseStatus.BAD_REQUEST, e);
		}
	}

	public Map<String, List<FileUpload>> files() throws IOException {
		decodeParams();
		return uploads;
	}

	public List<FileUpload> files(String name) throws IOException {
		decodeParams();
		return uploads.getOrDefault(name, Collections.emptyList());
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

	public boolean secure() {
		return context.pipeline().get("ssl") != null || "https".equalsIgnoreCase(request.headers().get("x-forwarded-proto"));
	}

	public long length() {
		try {
			return Long.parseLong(request.headers().get(HttpHeaderNames.CONTENT_LENGTH));
		} catch (Exception e) {
			return -1;
		}
	}

	public String host() {
		String host = request.headers().get("x-forwarded-host");
		if (host == null) {
			host = request.headers().get(HttpHeaderNames.HOST);
		}
		return host;
	}

	public int port() {
		String port = request.headers().get("x-forwarded-port");
		if (port != null) {
			return Integer.parseInt(port);
		} else {
			if (secure()) {
				return Config.getInt("server.securePort");
			} else {
				return Config.getInt("server.port");
			}
		}
	}

	public String remoteAddress() {
		String address = request.headers().get("x-forwarded-for");
		if (address != null) {
			return address.split(":")[0];
		} else {
			InetSocketAddress remoteAddress = (InetSocketAddress) context.channel().remoteAddress();
			return remoteAddress.getAddress().getHostAddress();
		}
	}

	public Session session() {
		return session(true);
	}

	public Session ifSession() {
		return session(false);
	}

	public boolean xhr() {
		return "XMLHttpRequest".equalsIgnoreCase(request.headers().get("x-requested-with"));
	}

	public String protocol() {
		return context.pipeline().get("h2") == null ? request.protocolVersion().text() : "HTTP/2";
	}

	public Map<String, Object> attributes() {
		return attributes;
	}

	public HttpContext set(String name, Object value) {
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

	public String securityAttribute() {
		return securityAttribute;
	}

	void setSecurityAttribute(String attribute) {
		securityAttribute = attribute;
	}

	void setGenericPath(String genericPath) {
		this.genericPath = genericPath;
	}

	private Map<String, List<String>> decodeParams() throws IOException {
		if (params == null) {
			params = new HashMap<>(query.parameters());
			uploads = new HashMap<>();
			CharSequence mimeType = HttpUtil.getMimeType(request);
			if (HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.contentEqualsIgnoreCase(mimeType) || HttpHeaderValues.MULTIPART_FORM_DATA.contentEqualsIgnoreCase(mimeType)) {
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
				} else if (type == boolean.class || type == Boolean.class) {
					return Boolean.parseBoolean(value);
				} else if (type == FileUpload.class) {
					return file(name);
				}
			}
		}
		throw new IllegalArgumentException("Unsupported type: " + targetType);
	}

	private Session session(boolean create) {
		if (session != null) {
			return session;
		}

		Cookie cookie = decodeCookies().get(SESSION_COOKIE_NAME);
		if (cookie != null) {
			String id = cookie.value();
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
					Cookie c = new DefaultCookie(SESSION_COOKIE_NAME, id);
					c.setDomain(host());
					c.setMaxAge(Cookie.UNDEFINED_MAX_AGE);
					c.setHttpOnly(true);
					responseCookies.put(SESSION_COOKIE_NAME, c);
					return session;
				}
			}
		}

		return null;
	}


	/* ---------------- Response -------------- */

	public HttpResponseStatus responseStatus() {
		return responseStatus;
	}

	public HttpContext responseStatus(HttpResponseStatus status) {
		responseStatus = status;
		return this;
	}

	public MediaType responseType() {
		return responseType;
	}

	public HttpContext responseType(MediaType type) {
		responseType = type;
		if (type.isText()) {
			responseHeaders.set(HttpHeaderNames.CONTENT_TYPE, type.name() + "; charset=utf-8");
		} else {
			responseHeaders.set(HttpHeaderNames.CONTENT_TYPE, type.name());
		}
		return this;
	}

	public HttpContext responseType(String path) {
		return responseType(MediaType.byPath(path).orElse(MediaType.OCTETSTREAM));
	}

	public HttpHeaders responseHeaders() {
		return responseHeaders;
	}

	public Map<String, Cookie> responseCookies() {
		return responseCookies;
	}

	public HttpContext removeCookie(String name) {
		Cookie cookie = new DefaultCookie(name, "");
		cookie.setMaxAge(0);
		responseCookies.put(name, cookie);
		return this;
	}

	public long responseLength() {
		return responseLength;
	}

	public HttpContext responseLength(long length) {
		responseLength = length;
		return this;
	}

	public Object redirect(String location) throws Exception {
		return redirect(location, Map.of());
	}

	public Object redirect(String location, Map<String, Object> params) throws Exception {
		return redirect(location, params, HttpResponseStatus.SEE_OTHER);
	}

	public Object redirect(String location, HttpResponseStatus status) throws Exception {
		return redirect(location, Map.of(), status);
	}

	public Object redirect(String location, Map<String, Object> params, HttpResponseStatus status) throws Exception {
		QueryStringEncoder encoder = new QueryStringEncoder(location);
		params.forEach((key, value) -> {
			if (value instanceof Iterable) {
				((Iterable<?>) value).forEach(val -> encoder.addParam(key, String.valueOf(val)));
			} else {
				encoder.addParam(key, String.valueOf(value));
			}
		});
		responseHeaders.set(HttpHeaderNames.LOCATION, encoder.toString());
		responseStatus = status;
		return commit(null);
	}

	public Object download(String filename, String location) throws Exception {
		URL url = IOUtil.resource(location);
		if (url == null) {
			throw new FileNotFoundException(location);
		}
		responseType(MediaType.byPath(location).orElse(MediaType.OCTETSTREAM));
		URLConnection con = url.openConnection();
		responseLength(con.getContentLengthLong());
		try (InputStream in = con.getInputStream()) {
			return download(filename, in);
		}
	}

	public Object download(String filename, Path file) throws Exception {
		responseType(MediaType.byPath(file).orElse(MediaType.OCTETSTREAM));
		responseLength(Files.size(file));
		return download(filename, Files.newInputStream(file));
	}

	public Object download(String filename, InputStream stream) throws Exception {
		String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8);
		responseHeaders.set(HttpHeaderNames.CONTENT_DISPOSITION, String.format("attachment; filename*=%s''%s", StandardCharsets.UTF_8.name(), encoded));
		return commit(stream);
	}

	public boolean committed() {
		return committed;
	}

	public Object commit(Object result) throws Exception {
		if (committed) {
			throw new IllegalStateException("Response has already been committed.");
		}
		if (result instanceof Result) {
			Result target = (Result) result;
			responseType(target.type());
			if (result instanceof Asset) {
				Asset asset = (Asset) result;
				if (CACHE_BY_ETAG) {
					String eTag = asset.eTag();
					responseHeaders.set(HttpHeaderNames.ETAG, eTag);
					if (eTag.equals(headers().get(HttpHeaderNames.IF_NONE_MATCH))) {
						responseStatus = HttpResponseStatus.NOT_MODIFIED;
					}
				}
				if (MAX_AGE > 0) {
					responseHeaders.set(HttpHeaderNames.CACHE_CONTROL, "max-age=" + MAX_AGE);
				}
			}
			send(target.stream());
		} else if (result == null) {
			send(Unpooled.EMPTY_BUFFER);
		} else if (result instanceof String) {
			send(Unpooled.wrappedBuffer(((String) result).getBytes(StandardCharsets.UTF_8)));
		} else if (result instanceof byte[]) {
			send(Unpooled.wrappedBuffer((byte[]) result));
		} else if (result instanceof InputStream) {
			send((InputStream) result);
		} else if (result instanceof ByteBuf) {
			send((ByteBuf) result);
		} else {
			throw new IllegalArgumentException("Unsupported result: " + result);
		}
		committed = true;
		return null;
	}

	private void send(InputStream in) throws Exception {
		byte[] chunk = new byte[bufferSize];
		int bytesRead = 0;
		while (bytesRead < bufferSize) {
			int read = in.read(chunk, bytesRead, bufferSize - bytesRead);
			if (read == -1) break; // end of stream
			bytesRead += read;
		}

		ByteBuf buffer = Unpooled.wrappedBuffer(chunk, 0, bytesRead);
		if (bytesRead < bufferSize) {
			send(buffer);
		} else {
			HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, responseStatus);
			if (!responseHeaders.contains(HttpHeaderNames.CONTENT_LENGTH)) {
				responseHeaders.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
			}
			encodeCookies();
			response.headers().set(responseHeaders);
			context.write(response);
			context.write(new DefaultHttpContent(buffer));
			ChannelFuture future = context.writeAndFlush(new HttpChunkedInput(new ChunkedStream(in, bufferSize)));
			future.addListener(future1 -> {
				if (future1.isSuccess()) {
					if (!keepAlive) {
						future.addListener(ChannelFutureListener.CLOSE);
					}
				} else {
					throw new WebException(future1.cause());
				}
			});
		}
	}

	private void send(ByteBuf buffer) {
		responseLength = buffer.readableBytes();
		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, responseStatus, buffer);
		responseHeaders.set(HttpHeaderNames.CONTENT_LENGTH, responseLength);
		encodeCookies();
		response.headers().set(responseHeaders);
		ChannelFuture future = context.writeAndFlush(response);
		if (!keepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private void encodeCookies() {
		for (Cookie cookie : responseCookies.values()) {
			if (cookie.domain() == null) {
				cookie.setDomain(host());
			}
			if (cookie.path() == null) {
				cookie.setPath(contextPath().isEmpty() ? "/" : contextPath());
			}
			String cookieString = ServerCookieEncoder.STRICT.encode(cookie);
			responseHeaders.add(HttpHeaderNames.SET_COOKIE, cookieString);
		}
	}
}
