Nebula Logging
==============

Lightweight Logging Framework with [SLF4J API](https://www.slf4j.org) Implementation


Features
--------

* Lightweight: **20 KB**
* Concise API
* Dynamic Configuration
* Console and File output with Rolling support
* SLF4J API implementation
* Simple Design: single static Logger


Documentation
-------------

### Syntax
```
[output[:options]]
[output[;options]]

output: stdout, stderr, /path/to/file.{}.log
options: level=info[,rolling=daily[,backups=60]]
level = trace, debug, info, warn, error, off
rolling = none, daily, monthly
backups = max number of old log files to keep

default:
output = stdout
level = info
rolling = none, file output only
backups = 30, file output only
```

### Apply Config
```
# Java API
Log.applyConfig("-Dnlog=/path/to/file.{}.log:rolling=daily,backups=60");

# system properties
java -Dnlog=/path/to/file.{}.log:rolling=daily,backups=60 -jar app.jar
```

### Usage
```java
Log.trace("Hello World.");
Log.debug("Hello World.");
Log.info("Hello {}.", "World");
Log.warn(() -> "Hello World.");
Log.error(new Throwable(), "Hello World.");
```
