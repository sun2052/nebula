Nebula
======

Nebula is a lightweight and easy to use web framework for Java 21+.


Features
--------

* Lightweight with zero dependencies
* Easy to use concise API
* Simple design with modern technologies
* Full stack framework with multiple practical modules


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


Modules
-------

### [Nebula Logging](nebula-logging)
Lightweight Logging Framework with [SLF4J API](https://www.slf4j.org) Implementation

### [Nebula Context](nebula-context)
Lightweight Dependency Injection ([JSR 330](https://www.jcp.org/en/jsr/detail?id=330)) Implementation

### [Nebula Proxy](nebula-proxy)
Lightweight Dynamic Proxy Generator using [JEP 457: Class-File API (Preview)](https://openjdk.org/jeps/457)

### [Nebula Utils](nebula-utils)
Lightweight Common Utilities

### [Nebula Socket](nebula-socket)
Lightweight Distributed Network Communication Framework

### [Nebula RPC](nebula-rpc)
Lightweight Remote Procedure Call (RPC) Framework

### [Nebula Web](nebula-web)
Lightweight Web MVC Framework and HTTP Server built with [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)


Build & Install
---------------

```
mvn clean install
```
