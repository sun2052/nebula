Nebula Socket
=============

Lightweight Distributed Network Communication Framework


Features
--------

* Lightweight: **11 KB**
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
// server address
var address = new InetSocketAddress("127.0.0.1", 2000);

// start server and waiting for incoming messages
new Thread(Unchecked.runnable(() -> {
    try (var endpoint = new Endpoint(address)) { // start server
        try (var node = endpoint.accept()) { // waiting for incoming connection...
            var msg = node.readMessage(); // waiting for incoming message...
            if (msg == null) { // if connection is closed
                System.out.println("Connection is closed by remote peer.");
                return;
            }
            System.out.println("received: " + new String(msg.bytes())); // print decoded message
            node.writeMessage(0, "bye".getBytes()); // send reply
        } // disconnect
    }
})).start();

// connect to server and send hello
new Thread(Unchecked.runnable(() -> {
    try (var node = new Node(address).connect()) { // connect to remote node
        node.writeMessage(0, "Hello".getBytes()); // say hello
        System.out.println("replied: " + new String(node.readMessage().bytes())); //  print decoded message
    } // close node
})).start();
```
