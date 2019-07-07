package org.byteinfo.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.EmptyHttpHeaders;
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
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2ConnectionEncoder;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.stream.ChunkedStream;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.AsciiString;
import org.byteinfo.util.io.IOUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class Response {
	private ChannelHandlerContext context;
	private FullHttpRequest request;
	private int bufferSize;
	private boolean keepAlive;

	private Map<String, Map<String, String>> pushList = new LinkedHashMap<>();
	private Map<String, Cookie> cookies = new HashMap<>();
	private HttpHeaders headers = new DefaultHttpHeaders();
	private HttpResponseStatus status = HttpResponseStatus.OK;
	private boolean committed;

	private long length = -1;
	private MediaType type = MediaType.OCTETSTREAM;
	private Object result;

	public Response(ChannelHandlerContext context, FullHttpRequest request, int bufferSize, String streamId) {
		this.context = context;
		this.request = request;
		this.bufferSize = bufferSize;
		this.keepAlive = HttpUtil.isKeepAlive(request);

		if (streamId != null) {
			headers.set(HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text(), streamId);
		}
	}

	public Response cookie(Cookie cookie) {
		cookies.put(cookie.name(), cookie);
		return this;
	}

	public Response clearCookie(String name) {
		Cookie cookie = new DefaultCookie(name, "");
		cookie.setMaxAge(0);
		cookies.put(name, cookie);
		return this;
	}

	public Response header(String name, String value) {
		headers.set(name, value);
		return this;
	}

	public Response header(CharSequence name, String value) {
		headers.set(name, value);
		return this;
	}

	public List<String> headers(String name) {
		List<String> headers = this.headers.getAll(name);
		return headers == null ? Collections.emptyList() : List.of(headers.toArray(new String[0]));
	}

	public String header(String name) {
		return headers.get(name);
	}

	public long length() {
		return length;
	}

	public Response length(long length) {
		this.length = length;
		return this;
	}

	public Optional<MediaType> type() {
		return Optional.ofNullable(type);
	}

	public Response type(MediaType type) {
		this.type = type;
		if (type.isText()) {
			headers.set(HttpHeaderNames.CONTENT_TYPE, type.name() + "; charset=utf-8");
		} else {
			headers.set(HttpHeaderNames.CONTENT_TYPE, type.name());
		}
		return this;
	}

	public Response type(String path) {
		return type(MediaType.byPath(path).orElse(MediaType.OCTETSTREAM));
	}

	public HttpResponseStatus status() {
		return status;
	}

	public Response status(HttpResponseStatus status) {
		this.status = requireNonNull(status, "Status required.");
		return this;
	}

	public Response status(int status) {
		return status(HttpResponseStatus.valueOf(status));
	}

	public Object result() {
		return result;
	}

	public void result(Object result) {
		this.result = result;
	}

	public void redirect(String location) throws Exception {
		redirect(HttpResponseStatus.FOUND, location);
	}

	public void redirect(HttpResponseStatus status, String location) throws Exception {
		this.status = status;
		headers.set(HttpHeaderNames.LOCATION, location);
		commit();
	}

	public void download(String filename, InputStream stream) throws Throwable {
		requireNonNull(filename, "A file's name is required.");
		requireNonNull(stream, "A stream is required.");

		type(filename);
		contentDisposition(filename);
		result(stream);
	}

	public void download(String location) throws Throwable {
		download(location, location);
	}

	public void download(String filename, String location) throws Throwable {
		URL url = IOUtil.getResource(location);
		if (url == null) {
			throw new FileNotFoundException(location);
		}
		type(type().orElseGet(() -> MediaType.byPath(filename).orElse(MediaType.byPath(location).orElse(MediaType.OCTETSTREAM))));
		URLConnection con = url.openConnection();
		length(con.getContentLengthLong());
		contentDisposition(filename);
		try (InputStream in = con.getInputStream()) {
			result(in);
		}
	}

	public void download(Path file) throws Throwable {
		download(file.getFileName().toString(), file);
	}

	public void download(String filename, Path file) throws Throwable {
		length(Files.size(file));
		download(filename, Files.newInputStream(file));
	}

	private void contentDisposition(String filename) throws IOException {
		String header = headers.get(HttpHeaderNames.CONTENT_DISPOSITION);
		if (header != null) {
			String basename = filename;
			int last = filename.lastIndexOf('/');
			if (last >= 0) {
				basename = basename.substring(last + 1);
			}
			String charset = StandardCharsets.UTF_8.name();
			String ebasename = URLEncoder.encode(basename, charset).replaceAll("\\+", "%20");
			header("Content-Disposition", String.format("attachment; filename=\"%s\"; filename*=%s''%s", basename, charset, ebasename));
		}
	}

	public Response push(String path) {
		return push(path, Map.of());
	}

	public Response push(String path, Map<String, String> headers) {
		if (context.pipeline().get("h2") != null) {
			pushList.put(path, headers);
			return this;
		} else {
			throw new UnsupportedOperationException("PUSH_PROMISE not supported.");
		}
	}

	public boolean committed() {
		return committed;
	}

	void commit() throws Exception {
		if (committed) {
			throw new IllegalStateException("Response has already been committed.");
		}
		if (result instanceof Result) {
			Result target = (Result) result;
			type(target.type());
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
			throw new IllegalArgumentException("Unsupported Result: " + result);
		}
		committed = true;
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
			HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
			if (!headers.contains(HttpHeaderNames.CONTENT_LENGTH)) {
				headers.set(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED);
			}
			if (this.keepAlive) {
				headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
			}

			// dump headers
			response.headers().set(headers);

			// add chunker
			ChannelPipeline pipeline = context.pipeline();
			if (pipeline.get("streamer") == null) {
				pipeline.addBefore("serverHandler", "streamer", new ChunkedWriteHandler());
			}

			// group all write
			context.channel().eventLoop().execute(() -> {
				// send headers
				context.write(response);
				// send head chunk
				context.write(new DefaultHttpContent(buffer));
				// send tail
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
			});
		}
	}

	private void send(ByteBuf buffer) {
		// PUSH_PROMISE must be sent before any response
		for (Map.Entry<String, Map<String, String>> entry : pushList.entrySet()) {
			String path = entry.getKey();
			Map<String, String> headers = entry.getValue();

			AsciiString streamIdHeader = HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text();
			Http2ConnectionEncoder encoder = context.pipeline().get(HttpToHttp2ConnectionHandler.class).encoder();
			Http2Connection connection = encoder.connection();
			int nextStreamId = connection.local().incrementAndGetNextStreamId();
			Http2Headers h2headers = new DefaultHttp2Headers()
					.path(path)
					.method(HttpMethod.GET.asciiName())
					.authority(Optional.ofNullable(request.headers().get(HttpHeaderNames.HOST)).orElseGet(() -> {
						InetSocketAddress localAddress = (InetSocketAddress) context.channel().localAddress();
						return localAddress.getAddress().getHostAddress();
					}))
					.scheme("https");
			headers.forEach(h2headers::add);
			encoder.writePushPromise(context, request.headers().getInt(streamIdHeader), nextStreamId, h2headers, 0, context.newPromise());

			DefaultFullHttpRequest pushRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1,
					HttpMethod.GET, path, Unpooled.EMPTY_BUFFER,
					new DefaultHttpHeaders(false).set(streamIdHeader, nextStreamId),
					EmptyHttpHeaders.INSTANCE);
			context.pipeline().fireChannelRead(pushRequest);
			context.pipeline().fireChannelReadComplete();
		}

		headers.remove(HttpHeaderNames.TRANSFER_ENCODING);
		headers.set(HttpHeaderNames.CONTENT_LENGTH, buffer.readableBytes());

		if (keepAlive) {
			headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
		}

		for (Cookie cookie : cookies.values()) {
			String cookieString = ServerCookieEncoder.STRICT.encode(cookie);
			headers.add(HttpHeaderNames.SET_COOKIE, cookieString);
		}

		FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status, buffer);
		response.headers().set(headers);

		ChannelFuture future = context.writeAndFlush(response);
		if (!keepAlive) {
			future.addListener(ChannelFutureListener.CLOSE);
		}
	}
}
