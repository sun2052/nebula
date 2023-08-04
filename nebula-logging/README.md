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

### Config Syntax
```
# stdout, stderr, /path/to/file.log.{}
output = stdout

# trace, debug, info, warn, error, off
level = info

# none, daily, monthly
rolling = none

# max number of old log files to keep
backups = 30
```

### Config Priority: from lowest to highest
```
# default config
org/byteinfo/logging/logging.properties

# custom config
logging.properties

# system properties: name1=value1[:name2=value2][:nameN=valueN]
java -Dlogging=output=/path/to/file.log.{}:rolling=daily:backups=30 -jar app.jar

# java api
Log.setLevel(Level.DEBUG);
Log.setWriter(new ConsoleWriter(System.err));
```

### Usage
```java
Log.trace("Hello World.");
Log.debug("Hello World.");
Log.info("Hello {}.", "World");
Log.warn(() -> "Hello World.");
Log.error(new Throwable(), "Hello World.");
```
