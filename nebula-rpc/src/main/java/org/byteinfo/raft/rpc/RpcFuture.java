package org.byteinfo.raft.rpc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RpcFuture<T> {
	private final CountDownLatch latch = new CountDownLatch(1);
	private volatile T result;

	public T get() throws InterruptedException {
		latch.await();
		return result;
	}

	public T get(long timeoutMillis) throws InterruptedException, TimeoutException {
		if (latch.await(timeoutMillis, TimeUnit.MILLISECONDS)) {
			return result;
		}
		throw new TimeoutException();
	}

	void set(T result) {
		this.result = result;
		latch.countDown();
	}
}
