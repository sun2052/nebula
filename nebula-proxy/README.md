Nebula Proxy
============

Lightweight Dynamic Proxy Generator using [JEP 457: Class-File API (Preview)](https://openjdk.org/jeps/457)


Features
--------

* Lightweight: **16 KB**
* Concise API: `Proxy.of(Target.class).with(aspects).instance()`
* Simple Design: **Runtime Subclass Generation**


Documentation
--------------

### Terminology
* Aspect: a pointcut and advice pair
* Pointcut: identify the method to be applied
* Advice: custom logic to be applied

### Compile Time and Run Time Options (Class-File API)
```
--enable-preview
```

### Usage
```java
Proxy.of(Target.class) // specify proxy target
	.with(Aspect.of(pointcut, MyAdvice.class)) // specify custom logic
	.debugPath(Path.of("/tmp")) // optional, output generated class file
	.instance(); // get generate proxy, create(): class file by byte[], load(): Class<?> obj, instance(): instance
```


Quick Start
-----------

```java
// proxy target
class Target {
	public String test(String str) {
		String obj = str + " World.";
		System.out.println("Executing: arg=%s, result=%s".formatted(str, obj));
		return obj;
	}
}

// pointcut
Pointcut pointcut = method -> method.methodName().equalsString("test");

// advice
class MyAdvice implements Advice {
	@Override
	public Object apply() throws Exception {
		String arg0 = (String) ProxyTarget.argument(0);
		String arg1 = "Hi";
		System.out.println("Before: arg=%s -> arg=%s".formatted(arg0, arg1));
		ProxyTarget.setArgument(arg1, 0);
		Object obj = ProxyTarget.proceed();
		System.out.println("After: result=%s".formatted(obj));
		return obj;
	}
}

// create proxy
Target t = Proxy.of(Target.class).with(Aspect.of(pointcut, MyAdvice.class)).instance();
System.out.println(">>> " + t.test("Hello"));
```
