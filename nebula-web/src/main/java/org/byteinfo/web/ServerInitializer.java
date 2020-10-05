package org.byteinfo.web;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http2.DefaultHttp2Connection;
import io.netty.handler.codec.http2.Http2ConnectionHandler;
import io.netty.handler.codec.http2.Http2FrameLogger;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandlerBuilder;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapterBuilder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolNegotiationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.EventExecutorGroup;
import org.byteinfo.util.misc.Config;

public class ServerInitializer extends ChannelInitializer<SocketChannel> {
	private final Server server;
	private final EventExecutorGroup executor;
	private final SslContext sslContext;
	private final boolean h2Enabled;
	private final boolean gzipEnabled;
	private final int httpTimeOut;
	private final int maxInitialLineLength;
	private final int maxHeaderSize;
	private final int maxChunkSize;
	private final int maxContentLength;

	public ServerInitializer(Server server, EventExecutorGroup executor, SslContext sslContext) {
		this.server = server;
		this.executor = executor;
		this.sslContext = sslContext;
		h2Enabled = Config.getBoolean("http.h2");
		gzipEnabled = Config.getBoolean("gzip.enabled");
		httpTimeOut = Config.getInt("http.timeout");
		maxInitialLineLength = Config.getInt("request.maxInitialLineLength");
		maxHeaderSize = Config.getInt("request.maxHeaderSize");
		maxChunkSize = Config.getInt("request.maxChunkSize");
		maxContentLength = Config.getInt("request.maxContentLength");
	}

	@Override
	protected void initChannel(SocketChannel ch) {
		ChannelPipeline p = ch.pipeline();
		if (sslContext != null) {
			p.addLast("ssl", sslContext.newHandler(ch.alloc()));
			p.addLast("h1.1/h2", new Http2OrHttpHandler());
		} else {
			configureHttp1(p);
		}
	}

	private void configureHttp2(ChannelPipeline p) {
		p.addLast("h2", newHttp2ConnectionHandler());
		idle(p);
		compressor(p);
		addServerHandler(p);
	}

	private void configureHttp1(ChannelPipeline p) {
		p.addLast("codec", new HttpServerCodec(maxInitialLineLength, maxHeaderSize, maxChunkSize, false));
		idle(p);
		p.addLast("aggregator", new HttpObjectAggregator(maxContentLength));
		compressor(p);
		addServerHandler(p);
	}

	private void idle(ChannelPipeline p) {
		if (httpTimeOut > 0) {
			p.addLast("timeout", new ReadTimeoutHandler(httpTimeOut));
		}
	}

	private void compressor(ChannelPipeline p) {
		if (gzipEnabled) {
			p.addLast("compressor", new CompressionHandler());
		}
	}

	private void addServerHandler(ChannelPipeline p) {
		p.addLast("streamer", new ChunkedWriteHandler());
		p.addLast(executor, "serverHandler", new ServerHandler(server));
	}

	private Http2ConnectionHandler newHttp2ConnectionHandler() {
		DefaultHttp2Connection connection = new DefaultHttp2Connection(true);
		InboundHttp2ToHttpAdapter listener = new InboundHttp2ToHttpAdapterBuilder(connection)
				.propagateSettings(false)
				.validateHttpHeaders(false)
				.maxContentLength(maxContentLength)
				.build();
		return new HttpToHttp2ConnectionHandlerBuilder()
				.frameListener(listener)
				.frameLogger(new Http2FrameLogger(LogLevel.DEBUG))
				.connection(connection)
				.build();
	}

	class Http2OrHttpHandler extends ApplicationProtocolNegotiationHandler {
		Http2OrHttpHandler() {
			super(ApplicationProtocolNames.HTTP_1_1);
		}

		@Override
		public void configurePipeline(ChannelHandlerContext ctx, String protocol) {
			if (h2Enabled && ApplicationProtocolNames.HTTP_2.equals(protocol)) {
				configureHttp2(ctx.pipeline());
			} else if (ApplicationProtocolNames.HTTP_1_1.equals(protocol)) {
				configureHttp1(ctx.pipeline());
			} else {
				throw new IllegalStateException("Unknown protocol: " + protocol);
			}
		}
	}
}
