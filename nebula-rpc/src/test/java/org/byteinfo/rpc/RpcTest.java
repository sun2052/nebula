package org.byteinfo.rpc;

import org.byteinfo.util.function.Unchecked;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class RpcTest {
	public static void main(String[] args) throws Exception {
		var serverAddress = new InetSocketAddress("127.0.0.1", 2000);

		Thread.startVirtualThread(Unchecked.runnable(() -> {
			var server = new RpcServer(serverAddress);
			server.addService(HelloService.class, new HelloImpl());
			server.start();

			TimeUnit.HOURS.sleep(1);
		}));

		TimeUnit.SECONDS.sleep(1);

		var client = new RpcClient(serverAddress).connect();
		var service = client.of(HelloService.class);

		for (int i = 0; i < 100; i++) {
			Thread.startVirtualThread(Unchecked.runnable(() -> {
				var req = String.valueOf(ThreadLocalRandom.current().nextInt(10000));
				var rsp = service.hello(req);
				System.out.printf("req=%s, rsp=%s%n", req, rsp);
			}));
		}

		TimeUnit.HOURS.sleep(1);
	}
}
