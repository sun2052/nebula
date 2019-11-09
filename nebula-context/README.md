# Nebula Context

Lightweight Dependency Injection ([JSR 330](https://www.jcp.org/en/jsr/detail?id=330)) Implementation


## Documentation

[Dependency Injection for Java (JSR 330)](https://javaee.github.io/javaee-spec/javadocs/javax/inject/package-summary.html)

* Default Scope: **@Singleton**
* @PostConstruct Methods
* ~~No Methods Injection~~


## Usage

```java
// initialize context
Context context = new Context();

// get an instance of A
A a = context.instance(A.class);

// get a provider of A
Provider<A> provider = context.provider(A.class);
```
