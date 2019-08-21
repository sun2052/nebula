# Nebula Web

Micro Web Framework


## Configuration

Configurations are located in the classpath.
```
# default config
org/byteinfo/web/application.properties

# custom config
application.properties
```

## Usage

```java
// Controller
public class MainController {
	@GET
	@Path("/")
	public Object index(HttpContext context) {
		return "Hello";
	}
}

// Bootstrap
new Server().handler(MainController.class).start();
```
