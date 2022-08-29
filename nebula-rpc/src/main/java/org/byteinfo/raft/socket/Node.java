package org.byteinfo.raft.socket;

import jdk.net.ExtendedSocketOptions;
import org.byteinfo.logging.Log;
import org.byteinfo.util.function.CheckedBiFunction;
import org.byteinfo.util.misc.Platform;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySession;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketImpl;
import java.net.SocketTimeoutException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_INT;

public class Node {
	private final Lock readLock = new ReentrantLock();
	private final Lock writeLock = new ReentrantLock();
	private final Address address;
	private volatile Socket socket;

	private volatile int connectTimeoutMillis = 5000; // socket connect timeout in millis
	private volatile int reconnectDelayMillis = 5000; // reconnect delay in millis
	private volatile int keepAliveIdleTime = 45; // max idle time in seconds before sending the probe
	private volatile int keepAliveProbeInterval = 5; // wait interval in seconds before sending another probe
	private volatile int keepAliveProbeCount = 3; // max probes count to be sent

	private volatile AtomicBoolean connected = new AtomicBoolean();
	private volatile AtomicBoolean connecting = new AtomicBoolean();

	public Node(Address address) {
		this.address = address;
	}

	public Node(Socket socket, Endpoint endpoint) throws IOException {
		setConnectTimeout(endpoint.connectTimeoutMillis(), endpoint.reconnectDelayMillis());
		setKeepAlive(endpoint.keepAliveIdleTime(), endpoint.keepAliveProbeInterval(), endpoint.keepAliveProbeCount());
		this.address = new Address(socket.getInetAddress().getHostAddress(), socket.getPort());
		this.socket = initialize(socket);
		connected.set(true);
	}

	public Node connect() throws InterruptedException, IOException {
		return connect(0);
	}

	public Node connect(int timeoutMillis) throws InterruptedException, IOException {
		long deadline = System.currentTimeMillis() + timeoutMillis;
		if (!connected.get() && connecting.compareAndSet(false, true)) {
			try {
				while (!connected.get()) {
					try {
						Log.info("Connecting to {}", address);
						var target = new InetSocketAddress(address.host(), address.port());
						if (timeoutMillis > 0) {
							timeoutMillis = Math.min(timeoutMillis, connectTimeoutMillis);
						}
						socket = new Socket();
						socket.connect(target, timeoutMillis);
						connected.set(true);
					} catch (SocketTimeoutException e) {
						Log.info("Connecting to {} timed out.", address);
					} catch (Exception e) {
						Log.info(e, "Connecting to {} failed.", address);
					}

					if (connected.get()) {
						initialize(socket);
					} else {
						if (timeoutMillis > 0 && deadline - System.currentTimeMillis() < reconnectDelayMillis) {
							break;
						} else {
							Log.info("Trying to reconnect in {} seconds.", new DecimalFormat("#.##").format(reconnectDelayMillis / 1000.0));
							Thread.sleep(reconnectDelayMillis);
						}
					}
				}
			} finally {
				connecting.set(false);
			}
		}
		return this;
	}

	public Node disconnect() throws IOException {
		return disconnect(false);
	}

	public Node disconnect(boolean forceClose) throws IOException {
		if (connected.compareAndSet(true, false)) {
			if (forceClose) {
				// socket.close() will send an RST instead of a FIN to avoid an unnecessary half-close
				socket.setSoLinger(true, 0);
			}
			// Linux: net.ipv4.tcp_fin_timeout = 60 (default)
			// This specifies how many seconds to wait for a final FIN packet before the socket is forcibly closed.
			String socketString = socket.toString();
			socket.close(); // send FIN or RST if forceClose
			socket = null;
			Log.info("Disconnected(forceClose={}): {}", forceClose, socketString);
		}
		return this;
	}

	/**
	 * Reads up to a specified number of bytes or end of stream is reached.
	 *
	 * @param maxLength the max number of bytes to read
	 * @return a byte array containing the bytes read
	 * @throws IOException if an io exception occurs
	 */
	public byte[] readMax(int maxLength) throws IOException, InterruptedException {
		ensureConnected();
		readLock.lockInterruptibly();
		try {
			return socket.getInputStream().readNBytes(maxLength);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Reads exactly a specified number of bytes.
	 *
	 * @param exactLength the exact number of bytes to read
	 * @return a byte array containing the bytes read
	 * @throws EOFException if end of stream is reached
	 * @throws IOException if an io exception occurs
	 */
	public byte[] readExact(int exactLength) throws IOException, InterruptedException {
		ensureConnected();
		readLock.lockInterruptibly();
		try {
			byte[] data = readMax(exactLength);
			if (data.length != exactLength) {
				throw new EOFException();
			}
			return data;
		} finally {
			readLock.unlock();
		}
	}

	public void write(byte[]... dataList) throws IOException, InterruptedException {
		ensureConnected();
		writeLock.lockInterruptibly();
		try {
			var out = socket.getOutputStream();
			for (byte[] data : dataList) {
				out.write(data);
			}
			out.flush();
		} finally {
			writeLock.unlock();
		}
	}

	public void write(InputStream in) throws IOException, InterruptedException {
		ensureConnected();
		writeLock.lockInterruptibly();
		try {
			var out = socket.getOutputStream();
			in.transferTo(out);
			out.flush();
		} finally {
			writeLock.unlock();
		}
	}

	public Node setConnectTimeout(int connectTimeoutMillis, int reconnectDelayMillis) {
		this.connectTimeoutMillis = connectTimeoutMillis;
		this.reconnectDelayMillis = reconnectDelayMillis;
		return this;
	}

	public Node setKeepAlive(int keepAliveIdleTime, int keepAliveProbeInterval, int keepAliveProbeCount) throws IOException {
		this.keepAliveIdleTime = keepAliveIdleTime;
		this.keepAliveProbeInterval = keepAliveProbeInterval;
		this.keepAliveProbeCount = keepAliveProbeCount;
		if (connected.get()) {
			setKeepAliveOptions(socket);
		}
		return this;
	}

	public boolean isConnected() {
		return connected.get();
	}

	public Address address() {
		return address;
	}

	public Socket socket() {
		return socket;
	}

	@Override
	public String toString() {
		return "@" + address;
	}

	private Socket initialize(Socket socket) throws IOException {
		Log.info("Connected: {}", socket);
		socket.setTcpNoDelay(true);
		socket.setKeepAlive(true);
		setKeepAliveOptions(socket);
		return socket;
	}

	private void setKeepAliveOptions(Socket socket) throws IOException {
		// JDK support for these options are only available on Linux and macOS.
		// https://bugs.openjdk.org/browse/JDK-8194298
		if (socket.supportedOptions().containsAll(List.of(ExtendedSocketOptions.TCP_KEEPIDLE, ExtendedSocketOptions.TCP_KEEPINTERVAL, ExtendedSocketOptions.TCP_KEEPCOUNT))) {
			socket.setOption(ExtendedSocketOptions.TCP_KEEPIDLE, keepAliveIdleTime);
			socket.setOption(ExtendedSocketOptions.TCP_KEEPINTERVAL, keepAliveProbeInterval);
			socket.setOption(ExtendedSocketOptions.TCP_KEEPCOUNT, keepAliveProbeCount);
		} else {
			if (Platform.isWindows()) {
				try {
					setKeepAliveOptionsForWindows(socket);
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
			} else {
				throw new RuntimeException("Keep-Alive Options are not supported.");
			}
		}
	}

	// These options are available starting with Windows 10, version 1709.
	// --enable-native-access=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED
	private void setKeepAliveOptionsForWindows(Socket socket) throws Throwable {
		// get native socket fd by deep reflection: Socket.impl -> SocketImpl.getFileDescriptor() -> FileDescriptor.fd
		CheckedBiFunction<String, Object, Object> getFieldValue = (name, obj) -> {
			var field = obj.getClass().getDeclaredField(name);
			field.setAccessible(true);
			return field.get(obj);
		};
		Object target = getFieldValue.apply("impl", socket);
		var method = SocketImpl.class.getDeclaredMethod("getFileDescriptor");
		method.setAccessible(true);
		target = method.invoke(target);
		int socketFd = (int) getFieldValue.apply("fd", target);

		// invoke windows native api
		try (MemorySession session = MemorySession.openConfined()) {
			// get method handle
			// https://docs.microsoft.com/en-us/windows/win32/api/winsock/nf-winsock-setsockopt
			var symbol = SymbolLookup.libraryLookup("Ws2_32.dll", session).lookup("setsockopt").orElseThrow();
			var function = FunctionDescriptor.of(JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT);
			MethodHandle methodHandle = Linker.nativeLinker().downcallHandle(symbol, function);

			// invoke method handle
			// https://docs.microsoft.com/en-us/windows/win32/winsock/ipproto-tcp-socket-options
			final int IPPROTO_TCP = 6;
			final int TCP_KEEPIDLE = 3;
			final int TCP_KEEPCNT = 16;
			final int TCP_KEEPINTVL = 17;
			int optionValueByteSize = (int) JAVA_INT.byteSize();
			for (Map.Entry<Integer, Integer> entry : Map.of(TCP_KEEPIDLE, keepAliveIdleTime, TCP_KEEPCNT, keepAliveProbeCount, TCP_KEEPINTVL, keepAliveProbeInterval).entrySet()) {
				int optionName = entry.getKey();
				var optionValue = session.allocate(JAVA_INT, entry.getValue());
				int ret = (int) methodHandle.invoke(socketFd, IPPROTO_TCP, optionName, optionValue, optionValueByteSize);
				if (ret != 0) {
					throw new RuntimeException("setsockopt() failed: ret=" + ret);
				}
			}
		}
	}

	private void ensureConnected() {
		if (!connected.get()) {
			throw new IllegalStateException("Node is not connected.");
		}
	}
}
