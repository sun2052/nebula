Nebula Utils
============

Lightweight Common Utilities


Features
--------

* Lightweight: **43 KB**
* Concise API
* Practical: **JDBC**, **WheelTimer**, **Exception Handling**, etc.


Documentation
-------------

### JDBC
```java
// data carrier
record User(int id, String name) {}

// retrieve a connection and execute a sql query
try (Connection con = DriverManager.getConnection(jdbc, username, password)) {
	List<User> list = JDBC.query(con, User.class, "select * from User where id > ?", 5);
}
```

### WheelTimer
```java
// execute the task after 1000ms
WheelTimer.getDefault().newTimeout(t -> {
	System.out.println("Hello, World!");
}, 1000);
```

### Exception Handling
```java
// ClassNotFoundException is a checked exception
Thread.startVirtualThread(() -> {
	try {
		Class.forName("java.lang.Object");
	} catch (ClassNotFoundException e) {
		throw new RuntimeException(e);
	}
});

// sneaky throws ClassNotFoundException
Thread.startVirtualThread(Unchecked.runnable(() -> {
	Class.forName("java.lang.Object");
}));
```
