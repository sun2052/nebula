package org.byteinfo.raft.rpc;

import org.byteinfo.logging.Log;
import org.byteinfo.raft.socket.Address;
import org.byteinfo.raft.socket.Node;
import org.byteinfo.util.codec.ByteUtil;
import org.byteinfo.util.function.Unchecked;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RpcClient implements AutoCloseable {
	private static final AtomicLong ID_GENERATOR = new AtomicLong();

	private final Map<Long, RpcFuture<RpcResponse>> map = new ConcurrentHashMap<>();

	private final Node node;

	private volatile boolean started;

	public RpcClient(Address remoteAddress) {
		node = new Node(remoteAddress);
	}

	public void connect() throws IOException, InterruptedException {
		started = true;
		node.connect();
		Thread.startVirtualThread(() -> {
			while (started) {
				try {
					var length = ByteUtil.asInt(node.readExact(Integer.BYTES));
					var data = node.readExact(length);
					var response = Serializer.deserialize(data, RpcResponse.class);
					var future = map.get(response.id());
					if (future != null) {
						future.set(response);
					}
				} catch (Exception e) {
					if (started) {
						Log.error(e, "Failed to receive message from {}, reconnecting...", node.address());
						Unchecked.runnable(() -> node.disconnect().connect()).run();
					}
				}
			}
		});
	}

	public <T> T of(Class<T> serviceInterface) {
		return (T) Proxy.newProxyInstance(serviceInterface.getClassLoader(), new Class<?>[] {serviceInterface}, (proxy, method, args) -> {
			var request = new RpcRequest(ID_GENERATOR.incrementAndGet(), serviceInterface.getName(), method.getName(), method.getParameterTypes(), args);
			var data = Serializer.serialize(request);
			node.write(ByteUtil.asBytes(data.length), data);
			var future = new RpcFuture<RpcResponse>();
			map.put(request.id(), future);
			var response = future.get();
			if (response.error() != null) {
				throw new RpcException(response.error());
			}
			return response.result();
		});
	}

	@Override
	public void close() throws Exception {
		started = false;
		node.disconnect();
	}
}
