Nebula Web
==========

Lightweight Web MVC Framework and HTTP Server built with [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)


Features
--------

* Lightweight: **48 KB**
* Concise API: **MVC API** and **Script API**
* Simple Design: **thread-per-request style** using **Virtual Threads**
* HTTP Server: **[HTTP/1.1](https://www.rfc-editor.org/rfc/rfc9112)**, **Chunked Transfer**, **Multipart Request**, **ETag**


Documentation
-------------

### Configurations are located in the classpath.
```
# default config
org/byteinfo/web/application.properties

# custom config
application.properties

# system properties
java -Dhttp.port=80 -jar app.jar
```


Quick Start
-----------

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

// Bootstrap
new Server()
	.handler(MainController.class)
	.start();
```

### Script API
```java
new Server()
	.get("/", ctx -> "Hello, World!")
	.start();
```

### Check the Result: `curl -i 127.0.0.1`
```
HTTP/1.1 200
content-type: text/plain; charset=utf-8
content-length: 13

Hello, World!
```
