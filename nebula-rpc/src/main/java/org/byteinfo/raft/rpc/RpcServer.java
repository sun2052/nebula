package org.byteinfo.raft.rpc;

import org.byteinfo.raft.socket.Address;
import org.byteinfo.raft.socket.Endpoint;
import org.byteinfo.raft.socket.Message;
import org.byteinfo.util.function.Unchecked;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RpcServer implements AutoCloseable {
	private final Endpoint endpoint;
	private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
	private volatile boolean started;

	public RpcServer(Address address) {
		endpoint = new Endpoint(address);
	}

	public void start() {
		started = true;
		endpoint.start();
		Thread.startVirtualThread(Unchecked.runnable(() -> {
			while (started) {
				handleMessage(endpoint.receive());
			}
		}));
	}

	public void addService(Class<?> service, Object serviceImpl) {
		serviceMap.put(service.getName(), serviceImpl);
	}

	private void handleMessage(Message message) {
		Thread.startVirtualThread(() -> {
			var request = Serializer.deserialize(message.data(), RpcRequest.class);

			try {
				var obj = serviceMap.get(request.service());
				if (obj == null) {
					throw new IllegalArgumentException("Service not found: " + request.service());
				}
				var method = obj.getClass().getMethod(request.method(), request.params());
				var result = method.invoke(obj, request.args());
				endpoint.send(message.origin(), Serializer.serialize(new RpcResponse(request.id(), result, null)));
			} catch (Exception e) {
				Throwable t = e;
				if (e instanceof InvocationTargetException && e.getCause() != null) {
					t = e.getCause();
				}
				endpoint.send(message.origin(), Serializer.serialize(new RpcResponse(request.id(), null, String.valueOf(t))));
			}
		});
	}

	@Override
	public void close() throws Exception {
		started = false;
		endpoint.stop();
	}
}
