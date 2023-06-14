package org.byteinfo.socket;

import jdk.net.ExtendedSocketOptions;
import org.byteinfo.logging.Log;
import org.byteinfo.util.codec.ByteUtil;
import org.byteinfo.util.function.CheckedBiFunction;
import org.byteinfo.util.io.LimitedInputStream;
import org.byteinfo.util.misc.Platform;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
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

public class Node implements Closeable {
	private static volatile int connectTimeoutMillis = 5000; // socket connect timeout in millis
	private static volatile int reconnectDelayMillis = 5000; // reconnect delay in millis
	private static volatile int bufferSize = 1024 * 1024 * 16; // send and receive buffer size in bytes
	private static volatile int keepAliveIdleTime = 45; // max idle time in seconds before sending the probe
	private static volatile int keepAliveProbeInterval = 5; // wait interval in seconds before sending another probe
	private static volatile int keepAliveProbeCount = 3; // max probes count to be sent

	private final AtomicBoolean connected = new AtomicBoolean();
	private final AtomicBoolean connecting = new AtomicBoolean();
	private final Lock readLock = new ReentrantLock();
	private final Lock writeLock = new ReentrantLock();
	private final InetSocketAddress address;
	private volatile Socket socket;

	public Node(InetSocketAddress address) {
		this.address = address;
	}

	public Node(Socket socket) throws IOException {
		this.address = (InetSocketAddress) socket.getRemoteSocketAddress();
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
						Log.debug("Connecting to {}", address);
						if (timeoutMillis > 0) {
							timeoutMillis = Math.min(timeoutMillis, connectTimeoutMillis);
						}
						socket = new Socket();
						socket.setReceiveBufferSize(bufferSize);
						socket.setSendBufferSize(bufferSize);
						socket.connect(address, timeoutMillis);
						connected.set(true);
					} catch (SocketTimeoutException e) {
						Log.debug("Connecting to {} timed out.", address);
					} catch (Exception e) {
						Log.debug(e, "Connecting to {} failed.", address);
					}

					if (connected.get()) {
						initialize(socket);
					} else {
						if (timeoutMillis > 0 && deadline - System.currentTimeMillis() < reconnectDelayMillis) {
							break;
						} else {
							Log.debug("Trying to reconnect in {} seconds.", new DecimalFormat("#.##").format(reconnectDelayMillis / 1000.0));
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

	public Node disconnect(boolean force) throws IOException {
		if (connected.compareAndSet(true, false)) {
			if (force) {
				// socket.close() will send an RST instead of a FIN to avoid an unnecessary half-close
				// https://docs.oracle.com/javase/8/docs/technotes/guides/net/articles/connection_release.html
				socket.setSoLinger(true, 0);
			}
			// Linux: net.ipv4.tcp_fin_timeout = 60 (default)
			// This specifies how many seconds to wait for a final FIN packet before the socket is forcibly closed.
			String socketString = socket.toString();
			socket.close(); // send FIN or RST if force=true
			socket = null;
			Log.debug("Disconnected(force={}): {}", force, socketString);
		}
		return this;
	}

	@Override
	public void close() throws IOException {
		disconnect();
	}

	/**
	 * Reads the message from the remote node.
	 *
	 * @return the message read or null if connection is closed
	 * @throws EOFException if end of stream is reached unexpectedly
	 * @throws IOException if an io error occurs
	 * @throws InterruptedException if current thread is interrupted
	 */
	public Message readMessage() throws IOException, InterruptedException {
		ensureConnected();
		readLock.lockInterruptibly();
		try {
			var in = socket.getInputStream();
			var bytes = in.readNBytes(Message.TYPE_SIZE);
			if (bytes.length == 0) {
				return null;
			}
			if (bytes.length != Message.TYPE_SIZE) {
				throw new EOFException();
			}
			var type = ByteUtil.asInt(bytes);
			var length = ByteUtil.asLong(readExact(in, Message.LENGTH_SIZE));
			return new Message(type, length, new LimitedInputStream(in, length), (InetSocketAddress) socket.getRemoteSocketAddress());
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Writes the type and data of the message to the remote node.
	 *
	 * @param type data type
	 * @param dataList byte arrays of the message data
	 * @throws IOException if an io error occurs
	 * @throws InterruptedException if current thread is interrupted
	 */
	public void writeMessage(int type, byte[]... dataList) throws IOException, InterruptedException {
		ensureConnected();
		writeLock.lockInterruptibly();
		try {
			var out = socket.getOutputStream();
			out.write(ByteUtil.asBytes(type));
			long length = 0;
			for (byte[] data : dataList) {
				length += data.length;
			}
			out.write(ByteUtil.asBytes(length));
			for (byte[] data : dataList) {
				out.write(data);
			}
			out.flush();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Writes the type and data of the message to the remote node.
	 *
	 * @param type data type
	 * @param length length of the data
	 * @param in input stream for reading the data
	 * @throws IOException if an io error occurs
	 * @throws InterruptedException if current thread is interrupted
	 */
	public void writeMessage(int type, long length, InputStream in) throws IOException, InterruptedException {
		ensureConnected();
		writeLock.lockInterruptibly();
		try {
			var out = socket.getOutputStream();
			out.write(ByteUtil.asBytes(type));
			out.write(ByteUtil.asBytes(length));
			in.transferTo(out);
			out.flush();
		} finally {
			writeLock.unlock();
		}
	}

	/**
	 * Reads up to a specified number of bytes or end of stream is reached.
	 *
	 * @param maxLength the max number of bytes to read
	 * @return a byte array containing the bytes read
	 * @throws IOException if an io error occurs
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
	 * @throws EOFException if end of stream is reached unexpectedly
	 * @throws IOException if an io error occurs
	 */
	public byte[] readExact(int exactLength) throws IOException, InterruptedException {
		ensureConnected();
		readLock.lockInterruptibly();
		try {
			return readExact(socket.getInputStream(), exactLength);
		} finally {
			readLock.unlock();
		}
	}

	/**
	 * Writes all bytes of the specified byte arrays.
	 *
	 * @param dataList byte arrays
	 * @throws IOException if an io error occurs
	 * @throws InterruptedException if current thread is interrupted
	 */
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

	/**
	 * Writes all bytes of the specified input stream.
	 *
	 * @param in input stream
	 * @throws IOException if an io error occurs
	 * @throws InterruptedException if current thread is interrupted
	 */
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

	public static void setConnectTimeout(int connectTimeoutMillis, int reconnectDelayMillis) {
		Node.connectTimeoutMillis = connectTimeoutMillis;
		Node.reconnectDelayMillis = reconnectDelayMillis;
	}

	public static void setBufferSize(int bufferSize) {
		Node.bufferSize = bufferSize;
	}

	public static void setKeepAlive(int keepAliveIdleTime, int keepAliveProbeInterval, int keepAliveProbeCount) throws IOException {
		Node.keepAliveIdleTime = keepAliveIdleTime;
		Node.keepAliveProbeInterval = keepAliveProbeInterval;
		Node.keepAliveProbeCount = keepAliveProbeCount;
	}

	public static int getConnectTimeoutMillis() {
		return connectTimeoutMillis;
	}

	public static int getReconnectDelayMillis() {
		return reconnectDelayMillis;
	}

	public static int getBufferSize() {
		return bufferSize;
	}

	public static int getKeepAliveIdleTime() {
		return keepAliveIdleTime;
	}

	public static int getKeepAliveProbeInterval() {
		return keepAliveProbeInterval;
	}

	public static int getKeepAliveProbeCount() {
		return keepAliveProbeCount;
	}

	public boolean isConnected() {
		return connected.get();
	}

	public InetSocketAddress address() {
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
		Log.debug("Connected: {}", socket);
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
		try (Arena arena = Arena.ofConfined()) {
			// get method handle
			// https://docs.microsoft.com/en-us/windows/win32/api/winsock/nf-winsock-setsockopt
			var symbol = SymbolLookup.libraryLookup("Ws2_32.dll", arena).find("setsockopt").orElseThrow();
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
				var optionValue = arena.allocate(JAVA_INT, entry.getValue());
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

	private byte[] readExact(InputStream in, int length) throws IOException {
		var bytes = in.readNBytes(length);
		if (bytes.length != length) {
			throw new EOFException();
		}
		return bytes;
	}
}
