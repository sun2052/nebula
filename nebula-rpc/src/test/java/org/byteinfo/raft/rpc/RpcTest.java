package org.byteinfo.raft.rpc;

import org.byteinfo.raft.socket.Address;
import org.byteinfo.util.function.Unchecked;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class RpcTest {
	public static void main(String[] args) throws Exception {
		var serverAddress = Address.of("127.0.0.1:2000");

		Thread.startVirtualThread(Unchecked.runnable(() -> {
			var server = new RpcServer(serverAddress);
			server.addService(HelloService.class, new HelloImpl());
			server.start();

			TimeUnit.HOURS.sleep(1);
		}));

		Thread.sleep(100);

		var client = new RpcClient(serverAddress);
		client.connect();
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
