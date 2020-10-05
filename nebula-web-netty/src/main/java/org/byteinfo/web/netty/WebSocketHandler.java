package org.byteinfo.web.netty;

import io.netty.buffer.ByteBuf;

public interface WebSocketHandler {
	default void onOpen(WebSocket ws) {}

	default void onTextMessage(WebSocket ws, String data) {}

	default void onBinaryMessage(WebSocket ws, ByteBuf data) {}

	default void onClose(WebSocket ws, int statusCode, String reasonText) {}

	default void onError(WebSocket ws, Throwable cause) {}
}
