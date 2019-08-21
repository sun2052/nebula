package org.byteinfo.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.ReferenceCountUtil;
import org.byteinfo.logging.Log;
import org.byteinfo.util.misc.Config;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	private static final int SSL_PORT = Config.getInt("ssl.port");
	private static final int BUFFER_SIZE = Config.getInt("response.bufferSize");
	private static final boolean SSL_ONLY = Config.getBoolean("ssl.enabled") && Config.getBoolean("ssl.only");
	private static final Handler ASSET_HANDLER = new AssetHandler();

	private final Server server;

	public ServerHandler(Server server) {
		this.server = server;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		try {
			if (msg instanceof FullHttpRequest) {
				long start = System.currentTimeMillis();
				FullHttpRequest req = (FullHttpRequest) msg;

				if (HttpUtil.is100ContinueExpected(req)) {
					ctx.write(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE));
				}

				HttpContext context = new HttpContext(ctx, req, BUFFER_SIZE);

				if (SSL_ONLY && !context.secure()) {
					String segment = "";
					if (SSL_PORT != 443) {
						segment = ":" + SSL_PORT;
					}
					context.redirect("https://" + context.host() + segment + req.uri(), HttpResponseStatus.MOVED_PERMANENTLY);
				}

				if (!context.committed()) {
					// exact handler
					Map<String, Handler> map = server.exactHandlers.getOrDefault(context.path(), Collections.emptyMap());
					Handler handler = map.get(context.method().name());

					// generic handler
					if (handler == null) {
						for (Map.Entry<String, Map<String, Handler>> entry : server.genericHandlers.entrySet()) {
							if (req.uri().startsWith(entry.getKey())) {
								handler = entry.getValue().get(context.method().name());
								break;
							}
						}
					}

					if (handler != null) {
						context.securityAttribute(server.securityAttributes.get(handler));
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
							server.errorHandler.handle(context, ex);
						} catch (Exception e2) {
							Log.error(e2, "ErrorHandler for '{} {}' resulted in exception.", context.method(), context.path());
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
							Log.error(e, "Interceptor for '{}' resulted in exception.", context.path());
						}
					}
				}
				AccessLog.log(context, start);
			}
		} finally {
			ReferenceCountUtil.release(msg);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Log.error(cause, "Unexpected error encountered.");
		ctx.close();
	}
}
