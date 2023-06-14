Nebula Context
==============

Lightweight Dependency Injection ([JSR 330](https://www.jcp.org/en/jsr/detail?id=330)) Implementation


Features
--------

* Lightweight: **15 KB**
* Concise API
* Simple Design


Documentation
-------------

### [Dependency Injection for Java (JSR 330)](https://javaee.github.io/javaee-spec/javadocs/javax/inject/package-summary.html)
* Default Scope: **Singleton**
* Fields Injection: **YES**
* Constructor Injection: **YES**
* PostConstruct Support: **YES**
* Methods Injection: **NO**

### Usage
```java
// config: map an interface to its implementation
class Config {
	@Provides
	DataSource dataSource() {
		return JdbcConnectionPool.create("jdbc:h2:~/test", "sa", "sa");
	}
}

// initialize context
Context context = new Context(new Config());

// get an instance of A
A a = context.instance(A.class);

// get a provider of A
Provider<A> provider = context.provider(A.class);

// get an instance of DataSource implementation
JdbcConnectionPool ds = context.instance(DataSource.class);
```
