package org.byteinfo.raft.socket;

import org.byteinfo.util.function.Unchecked;

import java.net.ServerSocket;

public class NodeTest {
	public static void main(String[] args) throws Exception {
		new Thread(Unchecked.runnable(() -> {
			var server = new ServerSocket(2000);
			server.setReuseAddress(true);
			server.accept();
		})).start();
	}
}
