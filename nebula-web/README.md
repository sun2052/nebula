Nebula Web
==========

Micro Web Framework


Configuration
-------------

Configurations are located in the classpath.
```
# default config
org/byteinfo/web/application.properties

# custom config
application.properties
```


Usage
-----

### MVC API
```java
// Controller
public class MainController {
	@GET
	@Path("/")
	public Object index(HttpContext context) {
		return "Hello, World!";
	} 
}

// Bootstrap
new Server().handler(MainController.class).start();
```

### Script API
```java
Server server = new Server();
server.get("/", ctx -> "Hello, World!");
server.start();
```
