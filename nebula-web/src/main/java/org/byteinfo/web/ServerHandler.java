package org.byteinfo.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.HttpConversionUtil;
import io.netty.util.ReferenceCountUtil;
import org.byteinfo.logging.Log;
import org.byteinfo.util.misc.Config;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Map;

public class ServerHandler extends ChannelInboundHandlerAdapter {
	private static final String STREAM_ID = HttpConversionUtil.ExtensionHeaderNames.STREAM_ID.text().toString();
	private static final Handler ASSET_HANDLER = new AssetHandler();

	private static final int SSL_PORT = Config.getInt("ssl.port");
	private static final int BUFFER_SIZE = Config.getInt("response.bufferSize");
	private static final boolean SSL_ONLY = Config.getBoolean("ssl.enabled") && Config.getBoolean("ssl.only");

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

				Request request = new Request(ctx, req);
				Response response = new Response(ctx, req, BUFFER_SIZE, req.headers().get(STREAM_ID));

				// sanitize request if necessary
				if (SSL_ONLY && !request.secure()) {
					String segment = "";
					if (SSL_PORT != 443) {
						segment = ":" + SSL_PORT;
					}
					response.redirect(HttpResponseStatus.MOVED_PERMANENTLY, "https://" + request.host() + segment + req.uri());
				}

				// determine request handler:
				// exact handler
				Map<String, Handler> map = server.exactHandlers.getOrDefault(request.path(), Collections.emptyMap());
				Handler handler = map.get(request.method().name());

				// generic handler
				if (handler == null) {
					for (Map.Entry<String, Map<String, Handler>> entry : server.genericHandlers.entrySet()) {
						if (req.uri().startsWith(entry.getKey())) {
							handler = entry.getValue().get(request.method().name());
							break;
						}
					}
				}

				if (handler != null) {
					request.setSecured(server.securedAttributes.get(handler));
				}

				// asset handler
				if (handler == null) {
					handler = ASSET_HANDLER;
				}

				Exception ex = null;
				try {
					// before
					for (Interceptor interceptor : server.interceptors) {
						if (!interceptor.before(request, response, handler)) {
							break;
						}
					}

					// business handler
					if (response.result() == null) {
						handler.handle(request, response);

						// after
						for (int i = server.interceptors.size() - 1; i >= 0; i--) {
							if (!server.interceptors.get(i).after(request, response, handler)) {
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
						server.errorHandler.handle(request, response, ex);
					} catch (Exception e2) {
						Log.error(e2, "ErrorHandler for '{} {}' resulted in exception.", request.method(), request.path());
						response.status(HttpResponseStatus.INTERNAL_SERVER_ERROR);
					}
				}

				// send response
				response.commit();

				// complete
				for (Interceptor interceptor : server.interceptors) {
					try {
						interceptor.complete(request, response, handler, ex);
					} catch (Exception e) {
						Log.error(e, "Interceptor for '{}' resulted in exception.", request.path());
					}
				}

				AccessLog.log(request, response, start);
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
