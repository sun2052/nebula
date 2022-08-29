package org.byteinfo.raft.socket;

import org.byteinfo.logging.Log;
import org.byteinfo.util.codec.ByteUtil;
import org.byteinfo.util.function.Unchecked;

import java.net.ServerSocket;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Endpoint {
	private final AtomicBoolean started = new AtomicBoolean();
	private final Set<Address> addressSet = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Map<Address, Node> nodeMap = new ConcurrentHashMap<>();
	private final BlockingQueue<Message> incoming = new ArrayBlockingQueue<>(1024);

	private volatile Address address;
	private volatile ServerSocket serverSocket;

	private volatile int connectTimeoutMillis = 5000; // socket connect timeout in millis
	private volatile int reconnectDelayMillis = 5000; // reconnect delay in millis
	private volatile int keepAliveIdleTime = 45; // max idle time in seconds before sending the probe
	private volatile int keepAliveProbeInterval = 5; // wait interval in seconds before sending another probe
	private volatile int keepAliveProbeCount = 3; // max probes count to be sent

	public Endpoint(Address address) {
		this.address = address;
	}

	public Endpoint start() {
		if (started.compareAndSet(false, true)) {
			Log.info("Endpoint({}) Started: listening on {}", address, address.port());
			Thread.startVirtualThread(Unchecked.runnable(() -> {
				serverSocket = new ServerSocket(address.port());
				serverSocket.setReuseAddress(true);
				while (started.get()) {
					var socket = serverSocket.accept();
					handleConnection(new Node(socket, this));
				}
			}));
		}
		return this;
	}

	public Endpoint stop() {
		return stop(false);
	}

	public Endpoint stop(boolean forceClose) {
		if (started.compareAndSet(true, false)) {
			Log.info("Stopping Endpoint({})...", address);
			try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
				for (Node node : nodeMap.values()) {
					executor.execute(Unchecked.runnable(() -> node.disconnect(forceClose)));
				}
			}
			nodeMap.clear();
			addressSet.clear();
			Log.info("Endpoint({}) Stopped.", address);
		}
		return this;
	}

	public Endpoint addNode(Address address) {
		ensureStarted();
		if (!address.equals(this.address)) {
			Log.info("Adding Node: {}", address);
			addressSet.add(address);
			Thread.startVirtualThread(Unchecked.runnable(() -> {
				while (started.get() && addressSet.contains(address) && !nodeMap.containsKey(address)) {
					var node = new Node(address);
					node.setConnectTimeout(connectTimeoutMillis, reconnectDelayMillis);
					node.setKeepAlive(keepAliveIdleTime, keepAliveProbeInterval, keepAliveProbeCount);
					node.connect();
					handleConnection(node);
				}
			}));
		}
		return this;
	}

	public Endpoint removeNode(Address address) {
		ensureStarted();
		Log.info("Removing Node: {}", address);
		addressSet.remove(address);
		var node = nodeMap.remove(address);
		if (node != null) {
			Thread.startVirtualThread(Unchecked.runnable(node::disconnect));
		}
		return this;
	}

	public Endpoint send(Address address, byte[] data) {
		var node = nodeMap.get(address);
		if (node != null) {
			Thread.startVirtualThread(() -> {
				try {
					node.write(ByteUtil.asBytes(data.length), data);
				} catch (Exception e) {
					Log.error(e, "Failed to send message to {}", address);
				}
			});
		}
		return this;
	}

	public Endpoint broadcast(byte[] data) {
		for (Address address : addressSet) {
			send(address, data);
		}
		return this;
	}

	public Message receive() throws InterruptedException {
		return incoming.take();
	}

	public Message receive(int timeoutMillis) throws InterruptedException {
		return incoming.poll(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	public Endpoint setConnectTimeout(int connectTimeoutMillis, int reconnectDelayMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
		this.reconnectDelayMillis = reconnectDelayMillis;
		for (Node node : nodeMap.values()) {
			node.setConnectTimeout(connectTimeoutMillis, reconnectDelayMillis);
		}
		return this;
	}

	public Endpoint setKeepAlive(int keepAliveIdleTime, int keepAliveProbeInterval, int keepAliveProbeCount) {
		this.keepAliveIdleTime = keepAliveIdleTime;
		this.keepAliveProbeInterval = keepAliveProbeInterval;
		this.keepAliveProbeCount = keepAliveProbeCount;
		for (Node node : nodeMap.values()) {
			Thread.startVirtualThread(Unchecked.runnable(() -> node.setKeepAlive(keepAliveIdleTime, keepAliveProbeInterval, keepAliveProbeCount)));
		}
		return this;
	}

	public boolean isStarted() {
		return started.get();
	}

	public Address address() {
		return address;
	}

	public int connectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public int reconnectDelayMillis() {
		return reconnectDelayMillis;
	}

	public int keepAliveIdleTime() {
		return keepAliveIdleTime;
	}

	public int keepAliveProbeInterval() {
		return keepAliveProbeInterval;
	}

	public int keepAliveProbeCount() {
		return keepAliveProbeCount;
	}

	private void handleConnection(Node node) {
		dropDuplicate(nodeMap.put(node.address(), node));
		Thread.startVirtualThread(() -> {
			while (started.get()) {
				try {
					var length = ByteUtil.asInt(node.readExact(Integer.BYTES));
					var data = node.readExact(length);
					incoming.put(new Message(data, node.address()));
				} catch (Exception e) {
					if (addressSet.contains(node.address())) {
						Log.error(e, "Failed to receive message from {}, reconnecting...", node.address());
						Unchecked.runnable(() -> node.disconnect().connect()).run();
					} else {
						Log.error(e, "Failed to receive message from {}, disconnecting...", node.address());
						nodeMap.remove(node.address());
						Unchecked.runnable(node::disconnect).run();
						break;
					}
				}
			}
		});
	}

	private void dropDuplicate(Node node) {
		Thread.startVirtualThread(Unchecked.runnable(() -> {
			if (node != null) {
				Log.info("Dropping Duplicate Connection: {}", node.socket());
				node.disconnect();
			}
		}));
	}

	private void ensureStarted() {
		if (!started.get()) {
			throw new IllegalStateException("Cluster Node is not started.");
		}
	}
}
