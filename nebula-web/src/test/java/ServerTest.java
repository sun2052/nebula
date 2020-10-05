import org.byteinfo.web.Server;
import org.byteinfo.web.WebSocket;
import org.byteinfo.web.WebSocketHandler;

import java.io.IOException;

public class ServerTest {
	public static void main(String[] args) throws IOException {
		new Server()
				.get("/", ctx -> "Hello, World!")
				.websocket("/ws", new WebSocketHandler() {
					@Override
					public void onTextMessage(WebSocket ws, String data) {
						ws.sendText("Received: " + data);
					}
				})
				.start();
	}
}
