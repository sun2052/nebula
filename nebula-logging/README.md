Nebula Logging
==============

Lightweight Logging Framework with [SLF4J API](https://www.slf4j.org) Implementation


Configuration
-------------

Configurations are located in the classpath.
```
# default config: writer* = <value>
org/byteinfo/logging/logging.properties

# custom config
logging.properties

# system properties
java -Dwriter.level=INFO -jar app.jar
```


Usage
-----

```java
Log.trace("Hello World.");
Log.debug("Hello World.");
Log.info("Hello {}.", "World");
Log.warn(() -> "Hello World.");
Log.error(new Throwable(), "Hello World.");
```
