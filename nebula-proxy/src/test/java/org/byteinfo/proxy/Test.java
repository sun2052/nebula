package org.byteinfo.proxy;

public class Test {
	public static void main(String[] args) {
		Pointcut pointcut = method -> method.methodName().equalsString("test");
		Target t = Proxy.of(Target.class).with(Aspect.of(pointcut, MyAdvice1.class), Aspect.of(pointcut, MyAdvice2.class)).instance();
		System.out.println(">>> " + t.test("Hello"));
	}

	static class Target {
		public String test(String str) {
			String obj = str + " World.";
			System.out.printf("Executing: arg=%s, result=%s%n", str, obj);
			return obj;
		}
	}

	static class MyAdvice1 implements Advice {
		@Override
		public Object apply() throws Exception {
			String arg0 = (String) ProxyTarget.argument(0);
			String arg1 = "Hi";
			System.out.printf("Advice1 Before: arg=%s -> arg=%s%n", arg0, arg1);
			ProxyTarget.setArgument(arg1, 0);
			Object obj = ProxyTarget.proceed();
			System.out.printf("Advice1 After: result=%s%n", obj);
			return obj;
		}
	}

	static class MyAdvice2 implements Advice {
		@Override
		public Object apply() throws Exception {
			String arg0 = (String) ProxyTarget.argument(0);
			String arg1 = "Hey";
			System.out.printf("Advice2 Before: arg=%s -> arg=%s%n", arg0, arg1);
			ProxyTarget.setArgument(arg1, 0);
			Object obj = ProxyTarget.proceed();
			System.out.printf("Advice2 After: result=%s%n", obj);
			return obj;
		}
	}
}
