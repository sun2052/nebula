package org.byteinfo.rpc;

import org.byteinfo.util.function.Unchecked;

import java.util.concurrent.ThreadLocalRandom;

public class HelloImpl implements HelloService {
	@Override
	public String hello(String name) {
		var n = ThreadLocalRandom.current().nextInt(1000);
		Unchecked.runnable(() -> Thread.sleep(n)).run();
		if (n > 900) {
			throw new RuntimeException("expected runtime exception");
		}
		return "Hello " + name;
	}
}
