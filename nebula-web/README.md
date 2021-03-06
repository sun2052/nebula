Nebula Web
==========

Minimal Web Framework based on [Project Loom](https://wiki.openjdk.java.net/display/loom/Main)

* HTTP/1.1


Configuration
-------------

Configurations are located in the classpath.
```
# default config
org/byteinfo/web/application.properties

# custom config
application.properties

# system properties
java -Dhttp.access=/opt/log/access.log -jar app.jar
```


Usage
-----

### MVC API
```java
// HTTP Controller
public class MainController {
	@GET
	@Path("/")
	public Object index(HttpContext context) {
		return "Hello, World!";
	}
}

// WebSocket Handler 
@Path("/ws")
public class MyWebSocketHandler implements WebSocketHandler {
	@Override
	public void onTextMessage(WebSocket ws, String data) {
		ws.sendText("Received: " + data);
	}
}

// Bootstrap
new Server()
.handler(MainController.class)
.websocket(MyWebSocketHandler.class)
.start();
```

### Script API
```java
new Server()
.get("/", ctx -> "Hello, World!")
.websocket("/ws", new WebSocketHandler() {
    @Override
    public void onTextMessage(WebSocket ws, String data) {
        ws.sendText("Received: " + data);
    }
})
.start();
```
