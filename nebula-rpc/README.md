Nebula RPC
==========

Lightweight Remote Procedure Call (RPC) Framework


Features
--------

* Lightweight: **12 KB**
* Concise API
* Modular Design


Documentation
-------------

### Required VM Options (Foreign Function & Memory API)
```
--enable-preview
```

### Extra VM Options (Windows Only for TCP Keep-Alive)
```
--enable-native-access=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
```


Quick Start
-----------

```java
// Service Definition
public interface HelloService {
	String hello(String name);
}

// Start RPC Server
var server = new RpcServer(Address.of("127.0.0.1:2000"));
server.addService(HelloService.class, new HelloImpl());
server.start();

// Start RPC Client and Send Request
var client = new RpcClient(serverAddress).connect();
var service = client.of(HelloService.class);
var rsp = service.hello(req);
System.out.printf("req=%s, rsp=%s%n", req, rsp);
```
