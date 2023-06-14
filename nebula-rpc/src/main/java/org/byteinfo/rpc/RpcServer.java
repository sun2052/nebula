package org.byteinfo.rpc;

import org.byteinfo.socket.Endpoint;
import org.byteinfo.socket.Node;
import org.byteinfo.util.function.Unchecked;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcServer implements AutoCloseable {
	private final Endpoint endpoint;
	private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
	private volatile boolean started;

	public RpcServer(InetSocketAddress address) throws IOException {
		endpoint = new Endpoint(address);
	}

	public void start() {
		started = true;
		new Thread(Unchecked.runnable(() -> {
			while (started) {
				var node = endpoint.accept();
				Thread.startVirtualThread(Unchecked.runnable(() -> handleConnection(node)));
			}
		})).start();
	}

	public <T> void addService(Class<T> service, T serviceImpl) {
		serviceMap.put(service.getName(), serviceImpl);
	}

	private void handleConnection(Node node) throws IOException, InterruptedException {
		while (true) {
			var message = node.readMessage();
			if (message == null) {
				break;
			}
			var request = Serializer.deserialize(message.bytes(), RpcRequest.class);

			try {
				var obj = serviceMap.get(request.service());
				if (obj == null) {
					throw new IllegalArgumentException("Service not found: " + request.service());
				}
				var method = obj.getClass().getMethod(request.method(), request.params());
				var result = method.invoke(obj, request.args());
				node.writeMessage(0, Serializer.serialize(new RpcResponse(request.id(), result, null)));
			} catch (Exception e) {
				Throwable t = e;
				if (e instanceof InvocationTargetException && e.getCause() != null) {
					t = e.getCause();
				}
				node.writeMessage(0, Serializer.serialize(new RpcResponse(request.id(), null, String.valueOf(t))));
			}
		}
	}

	@Override
	public void close() throws Exception {
		started = false;
		endpoint.close();
	}
}
