Nebula Proxy
============

Lightweight [ASM-Based](https://asm.ow2.io/) Runtime Proxy Generator


Feature
-------

* Minimal Overhead: **140 KB**
* Concise API: `Proxy.of(Target.class).with(aspects).instance()`
* Simple Design: **Runtime Subclass Generation**


Documentation
--------------

### Terminology

* Aspect:  a pointcut and advice pair
* Pointcut: identify the method to be applied
* Advice: custom logic to be applied

### Usage

```java
Proxy.of(Target.class) // specify proxy target
		.with(Aspect.of(pointcut, MyAdvice.class)) // specify custom logic
        .debugPath(Path.of("/tmp/generated.class")) // optional, output generated class file
        .instance(); // get generate proxy, create(): class file by byte[], load(): Class<?> obj, instance(): instance
```


Demo
----

```java
// proxy target
class Target {
    public String test(String str) {
        String obj = str + " World.";
        System.out.println(String.format("Executing: arg=%s, result=%s", str, obj));
        return obj;
    }
}

// pointcut
Pointcut pointcut = info -> info.name().equals("test");

// advice
class MyAdvice implements Advice {
    @Override
    public Object apply() throws Exception {
        String arg0 = (String) ProxyTarget.argument(0);
        String arg1 = "Hi";
        System.out.println(String.format("Before: arg=%s -> arg=%s", arg0, arg1));
        ProxyTarget.setArgument(arg1, 0);
        Object obj = ProxyTarget.proceed();
        System.out.println(String.format("After: result=%s", obj));
        return obj;
    }
}

// create proxy
Target t = Proxy.of(Target.class).with(Aspect.of(pointcut, MyAdvice.class)).instance();
System.out.println(">>> " + t.test("Hello"));
```
