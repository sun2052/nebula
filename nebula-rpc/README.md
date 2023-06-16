Nebula RPC
==========

Lightweight Remote Procedure Call (RPC) Framework


Features
--------

* Lightweight: **12 KB**
* Concise API
* Modular Design


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
