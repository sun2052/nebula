Nebula Proxy
============

Lightweight [ASM-Based](https://asm.ow2.io/) Runtime Proxy Generator


Usage
-----

```java
// proxy target
class Target {
    public String test(String str) {
        String obj = str + " World.";
        System.out.println(String.format("Executing: arg=%s, result=%s", str, obj));
        return obj;
    }
}

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
Pointcut pointcut = info -> info.name().equals("test");
Target t = Proxy.of(Target.class).with(Aspect.of(pointcut, MyAdvice.class)).instance();
System.out.println(">>> " + t.test("Hello"));
```
