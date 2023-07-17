package org.byteinfo.socket;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class Endpoint implements Closeable {
	private final ServerSocket serverSocket;

	public Endpoint(InetSocketAddress address) throws IOException {
		serverSocket = new ServerSocket();
		serverSocket.setReuseAddress(true);
		serverSocket.setReceiveBufferSize(Node.getBufferSize());
		serverSocket.bind(address);
	}

	public Node accept() throws IOException {
		var socket = serverSocket.accept();
		socket.setSendBufferSize(Node.getBufferSize());
		return new Node(socket);
	}

	@Override
	public void close() throws IOException {
		serverSocket.close();
	}

	public ServerSocket serverSocket() {
		return serverSocket;
	}
}
