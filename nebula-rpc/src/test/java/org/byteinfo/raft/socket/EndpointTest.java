package org.byteinfo.raft.socket;

import org.byteinfo.logging.Log;
import org.byteinfo.util.function.Unchecked;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class EndpointTest {
	public static void main(String[] args) throws Exception {
		var a1 = new Address("127.0.0.1", 2001);
		var a2 = new Address("127.0.0.1", 2002);
		var a3 = new Address("127.0.0.1", 2003);
		var nodes = List.of(a1, a2, a3);
		var counter = new AtomicLong();

		Thread.startVirtualThread(Unchecked.runnable(() -> {
			var c = new Endpoint(a1).start();
			nodes.forEach(c::addNode);

			Thread.startVirtualThread(Unchecked.runnable(() -> {
				while (true) {
					var msg = c.receive();
					Log.info("{} receiving from {}: {}", c.address(), msg.origin(), new String(msg.data()));
				}
			}));

			while (true) {
				c.broadcast(("Hello from a1 " + counter.incrementAndGet()).getBytes());
				TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(5));
			}
		}));

		Thread.startVirtualThread(Unchecked.runnable(() -> {
			var c = new Endpoint(a2).start();
			nodes.forEach(c::addNode);

			Thread.startVirtualThread(Unchecked.runnable(() -> {
				while (true) {
					var msg = c.receive();
					Log.info("{} receiving from {}: {}", c.address(), msg.origin(), new String(msg.data()));
				}
			}));

			while (true) {
				c.broadcast(("Hello from a2 " + counter.incrementAndGet()).getBytes());
				TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(5));
			}
		}));

		Thread.startVirtualThread(Unchecked.runnable(() -> {
			var c = new Endpoint(a3).start();
			nodes.forEach(c::addNode);

			Thread.startVirtualThread(Unchecked.runnable(() -> {
				while (true) {
					var msg = c.receive();
					Log.info("{} receiving from {}: {}", c.address(), msg.origin(), new String(msg.data()));
				}
			}));

			while (true) {
				c.broadcast(("Hello from a3 " + counter.incrementAndGet()).getBytes());
				TimeUnit.SECONDS.sleep(ThreadLocalRandom.current().nextInt(5));
			}
		}));


		TimeUnit.HOURS.sleep(1);
	}
}
