Nebula RPC
==========

Lightweight Network Communications and RPC Framework


Features
--------

* Lightweight: **25 KB**
* Concise API
* Modular Design


Documentation
-------------

### Socket
A minimal framework for easy network communications.

* auto reconnect on disconnection
* configurable native TCP Keep-Alive (Linux, Mac & Windows only)
* dynamic adding or removing node from endpoint
* easy sending and receiving data

Extra VM Options required for TCP Keep-Alive on Windows Platform
```
--enable-native-access=ALL-UNNAMED
--add-opens=java.base/java.net=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
```

### RPC
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
var client = new RpcClient(serverAddress);
client.connect();
var service = client.of(HelloService.class);
var rsp = service.hello(req);
System.out.printf("req=%s, rsp=%s%n", req, rsp);
```
