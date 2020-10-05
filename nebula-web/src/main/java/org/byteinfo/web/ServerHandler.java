package org.byteinfo.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.byteinfo.logging.Log;
import org.byteinfo.util.misc.Config;

import java.io.EOFException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;

public class ServerHandler extends SimpleChannelInboundHandler<Object> {
	public static final AttributeKey<String> PATH = AttributeKey.newInstance(ServerHandler.class.getName());

	private static final int PORT = Config.getInt("http.port");
	private static final int SSL_PORT = Config.getInt("ssl.port");
	private static final int HTTP_BUFFER_SIZE = Config.getInt("response.bufferSize");
	private static final int WS_MAX_CONTENT_LENGTH = Config.getInt("ws.maxContentLength");
	private static final boolean SSL_ONLY = Config.getBoolean("ssl.enabled") && Config.getBoolean("ssl.only");
	private static final boolean NO_WWW_PREFIX = Config.getBoolean("http.noWwwPrefix");
	private static final boolean NO_TRAILING_SLASH = Config.getBoolean("http.noTrailingSlash");
	private static final boolean REQUIRE_SANITIZE = SSL_ONLY || NO_WWW_PREFIX || NO_TRAILING_SLASH;
	private static final Handler ASSET_HANDLER = new AssetHandler();

	private final Server server;

	public ServerHandler(Server server) {
		this.server = server;
	}

	@Override
	public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			long start = System.currentTimeMillis();
			FullHttpRequest req = (FullHttpRequest) msg;
			ctx.channel().attr(PATH).set(req.method().name() + " " + req.uri());
			HttpContext context = new HttpContext(ctx, req, HTTP_BUFFER_SIZE);
			try {
				handleHttpRequest(ctx, req, context);
			} finally {
				AccessLog.log(context, start);
			}
		} else if (msg instanceof WebSocketFrame) {
			ctx.channel().attr(WebSocket.KEY).get().handle((WebSocketFrame) msg);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		try {
			Attribute<WebSocket> ws = ctx.channel().attr(WebSocket.KEY);
			if (ws != null && ws.get() != null) {
				ws.get().handle(cause);
			} else {
				String msg = "Unexpected error encountered while handling: " + ctx.channel().attr(PATH).get();
				if (isConnectionLost(cause)) {
					Log.debug(cause, msg);
				} else {
					Log.error(cause, msg);
				}
			}
		} finally {
			ctx.close();
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req, HttpContext context) throws Exception {
		if (HttpUtil.is100ContinueExpected(req)) {
			ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
		}

		// sanitize request if necessary
		if (REQUIRE_SANITIZE) {
			boolean redirect = false;
			String scheme = "http://";
			String host = context.host();
			int port = PORT;
			String uri = req.uri();
			if (SSL_ONLY) {
				scheme = "https://";
				port = SSL_PORT;
				if (!context.secure()) {
					redirect = true;
				}
			}
			if (NO_WWW_PREFIX && host.startsWith("www.")) {
				host = host.substring(4);
				redirect = true;
			}
			if (NO_TRAILING_SLASH && uri.length() > 1 && uri.endsWith("/")) {
				uri = uri.substring(0, uri.length() - 1);
				redirect = true;
			}
			if (redirect) {
				String segment = "";
				if (port != 80 && port != 443) {
					segment = ":" + port;
				}
				context.redirect(scheme + host + segment + uri, HttpResponseStatus.MOVED_PERMANENTLY);
				return;
			}
		}

		if (!server.webSocketHandlers.isEmpty() && "WebSocket".equalsIgnoreCase(req.headers().get(HttpHeaderNames.UPGRADE))) {
			WebSocketHandler handler = server.webSocketHandlers.get(context.realPath());
			if (handler != null) {
				String location = (context.secure() ? "wss" : "ws") + "://" + context.host() + context.requestPath();
				WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(location, null, true, WS_MAX_CONTENT_LENGTH);
				WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
				if (handshaker == null) {
					WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
				} else {
					ctx.pipeline().addAfter("codec", "wsCompressor", new WebSocketServerCompressionHandler());
					handshaker.handshake(ctx.channel(), req);
					WebSocket webSocket = new WebSocket(handler, handshaker, ctx, context.realPath(), context.headers());
					ctx.channel().attr(WebSocket.KEY).set(webSocket);
					handler.onOpen(webSocket);
				}
				return;
			}
		}

		// exact handler
		Map<String, Handler> map = server.exactHandlers.getOrDefault(context.realPath(), Collections.emptyMap());
		Handler handler = map.get(context.method().name());

		// generic handler
		if (handler == null) {
			for (Map.Entry<String, Map<String, Handler>> entry : server.genericHandlers.entrySet()) {
				if (req.uri().startsWith(entry.getKey())) {
					handler = entry.getValue().get(context.method().name());
					context.setGenericPath(context.requestPath().substring(entry.getKey().length()));
					break;
				}
			}
		}

		if (handler != null) {
			context.setSecurityAttribute(server.securityAttributes.get(handler));
		}

		// asset handler
		if (handler == null) {
			handler = ASSET_HANDLER;
		}

		Object result = null;
		Exception ex = null;
		try {
			// before
			for (Interceptor interceptor : server.interceptors) {
				interceptor.before(context, handler);
				if (context.committed()) {
					break;
				}
			}

			// business handler
			if (!context.committed()) {
				result = handler.handle(context);

				// after
				for (int i = server.interceptors.size() - 1; i >= 0; i--) {
					server.interceptors.get(i).after(context, handler, result);
					if (context.committed()) {
						break;
					}
				}
			}
		} catch (Exception e) {
			// strip unnecessary exception
			if (e.getClass() == InvocationTargetException.class && e.getCause() != null) {
				ex = (Exception) e.getCause();
			} else {
				ex = e;
			}

			try {
				result = server.errorHandler.handle(context, ex);
			} catch (Exception e2) {
				Log.error(e2, "ErrorHandler for '{} {}' resulted in exception.", context.method(), context.requestPath());
				context.responseStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
			}
		}

		// send response
		if (!context.committed()) {
			context.commit(result);
		}

		// complete
		for (Interceptor interceptor : server.interceptors) {
			try {
				interceptor.complete(context, handler, ex);
			} catch (Exception e) {
				Log.error(e, "Interceptor for '{}' resulted in exception.", context.requestPath());
			}
		}
	}

	private boolean isConnectionLost(Throwable cause) {
		if (cause instanceof IOException) {
			String message = cause.getMessage();
			if (message != null) {
				String msg = message.toLowerCase();
				return msg.contains("reset by peer") || msg.contains("broken pipe");
			}
		}
		return cause instanceof ReadTimeoutException || cause instanceof ClosedChannelException || cause instanceof EOFException;
	}
}
