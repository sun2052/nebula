package org.byteinfo.web;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.AttributeKey;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocket {
	public static final AttributeKey<WebSocket> KEY = AttributeKey.newInstance(WebSocket.class.getName());
	public static final Map<String, Set<WebSocket>> SESSIONS = new ConcurrentHashMap<>();

	private Map<String, Object> attributes = new ConcurrentHashMap<>();
	private WebSocketHandler handler;
	private WebSocketServerHandshaker handshaker;
	private ChannelHandlerContext context;
	private String path;
	private HttpHeaders headers;

	public WebSocket(WebSocketHandler handler, WebSocketServerHandshaker handshaker, ChannelHandlerContext context, String path, HttpHeaders headers) {
		this.handler = handler;
		this.handshaker = handshaker;
		this.context = context;
		this.path = path;
		this.headers = headers;
		SESSIONS.computeIfAbsent(path, k -> Collections.newSetFromMap(new ConcurrentHashMap<>())).add(this);
	}

	public WebSocket sendBinary(ByteBuf data) {
		context.channel().writeAndFlush(new BinaryWebSocketFrame(data));
		return this;
	}

	public WebSocket sendBinary(byte[] data) {
		return sendBinary(Unpooled.wrappedBuffer(data));
	}

	public WebSocket sendText(String data) {
		context.channel().writeAndFlush(new TextWebSocketFrame(data));
		return this;
	}

	public boolean isOpen() {
		return context.channel().isOpen();
	}

	public WebSocket close() {
		return close(WebSocketCloseStatus.NORMAL_CLOSURE);
	}

	public WebSocket close(WebSocketCloseStatus closeStatus) {
		return close(closeStatus.code(), closeStatus.reasonText());
	}

	public WebSocket close(int statusCode, String reasonText) {
		removeSession();
		context.channel().writeAndFlush(new CloseWebSocketFrame(statusCode, reasonText));
		handler.onClose(this, statusCode, reasonText);
		return this;
	}

	public Map<String, Object> attributes() {
		return attributes;
	}

	public WebSocket set(String name, Object value) {
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

	public Set<WebSocket> getSessions() {
		return SESSIONS.getOrDefault(path, Set.of());
	}

	void removeSession() {
		Set<WebSocket> set = getSessions();
		if (!set.isEmpty()) {
			set.remove(this);
		}
	}

	void handle(WebSocketFrame webSocketFrame) {
		ChannelFuture future = null;
		if (webSocketFrame instanceof CloseWebSocketFrame) {
			CloseWebSocketFrame frame = (CloseWebSocketFrame) webSocketFrame;
			handler.onClose(this, frame.statusCode(), frame.reasonText());
			future = handshaker.close(context.channel(), frame.retain());
		} else if (webSocketFrame instanceof PingWebSocketFrame) {
			future = context.write(new PongWebSocketFrame(webSocketFrame.content().retain()));
		} else if (webSocketFrame instanceof TextWebSocketFrame) {
			TextWebSocketFrame frame = (TextWebSocketFrame) webSocketFrame;
			handler.onTextMessage(this, frame.text());
		} else if (webSocketFrame instanceof BinaryWebSocketFrame) {
			BinaryWebSocketFrame frame = (BinaryWebSocketFrame) webSocketFrame;
			handler.onBinaryMessage(this, frame.content());
		}
		if (future != null) {
			future.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE).addListener(ChannelFutureListener.CLOSE);
		}
	}

	void handle(Throwable cause) {
		handler.onError(this, cause);
		handle(new CloseWebSocketFrame(WebSocketCloseStatus.INTERNAL_SERVER_ERROR));
	}
}
