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
java -Dhttp.port=80 -jar app.jar
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
