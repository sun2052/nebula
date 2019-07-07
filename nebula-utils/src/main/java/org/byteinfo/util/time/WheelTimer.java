package org.byteinfo.util.time;

import org.byteinfo.util.function.CheckedConsumer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A WheelTimer optimized for approximated I/O timeout scheduling.
 */
public class WheelTimer {
	private final Bucket[] wheel;
	private final long duration;
	private volatile boolean stopped;
	final Queue<Timeout> pending = new ConcurrentLinkedQueue<>();
	final Queue<Timeout> cancelled = new ConcurrentLinkedQueue<>();
	final CheckedConsumer<Exception> exceptionHandler;

	public WheelTimer() {
		this(1, 3600);
	}

	public WheelTimer(int precision, int wheelSize) {
		this(precision, wheelSize, e -> {
		});
	}

	/**
	 * Creates a new timer.
	 *
	 * @param precision precision in seconds
	 * @param wheelSize the size of the wheel
	 * @param exceptionHandler exception handler for scheduled task
	 */
	public WheelTimer(int precision, int wheelSize, CheckedConsumer<Exception> exceptionHandler) {
		if (precision <= 0) {
			throw new IllegalArgumentException("precision must be greater than 0: " + precision);
		}
		if (wheelSize <= 0) {
			throw new IllegalArgumentException("wheelSize must be greater than 0: " + wheelSize);
		}
		int size = wheelSize + 1;
		duration = precision * 1000L;
		wheel = new Bucket[size];
		for (int i = 0; i < size; i++) {
			wheel[i] = new Bucket();
		}
		this.exceptionHandler = exceptionHandler;

		Thread worker = new Thread(() -> {
			int tick = 0;
			while (!stopped) {
				long currentTime = System.currentTimeMillis();
				Timeout timeout;

				// process all pending tasks and add to correct positions if necessary
				while ((timeout = pending.poll()) != null) {
					if (timeout.state() == Timeout.ST_CANCELLED) {
						continue;
					}
					if (timeout.deadline <= currentTime) {
						timeout.expire();
					} else {
						int index = (int) ((((timeout.deadline - currentTime) / duration) + tick + 1) % size);
						wheel[index].add(timeout);
					}
				}

				// process all tasks in current position
				wheel[tick].expire(currentTime);

				// clear all cancelled Timeouts
				while ((timeout = cancelled.poll()) != null) {
					timeout.bucket.remove(timeout);
				}

				// wait for the next tick
				long time = duration - (System.currentTimeMillis() - currentTime);
				if (time > 0) {
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}

				tick++;
				if (tick == size) {
					tick -= size;
				}
			}
		}, "WheelTimer");
		worker.setDaemon(true);
		worker.start();
	}

	/**
	 * Schedules the specified task for one-time execution after the specified delay.
	 *
	 * @param task task to be executed
	 * @param delay delay in seconds
	 * @return a handle which is associated with the specified task
	 */
	public Timeout newTimeout(CheckedConsumer<Timeout> task, int delay) {
		if (stopped) {
			throw new IllegalStateException("WheelTimer has been stopped.");
		}
		long deadline = System.currentTimeMillis() + delay * 1000L;
		Timeout timeout = new Timeout(task, this, deadline);
		pending.add(timeout);
		return timeout;
	}

	/**
	 * Stops the WheelTimer and ignores all pending tasks.
	 */
	public void stop() {
		stopped = true;
	}

	@Override
	public String toString() {
		return "WheelTimer{" + "precision=" + duration / 1000 + "s, wheelSize=" + wheel.length + ", stopped=" + stopped + '}';
	}

	static class Bucket {
		Timeout head;
		Timeout tail;

		void add(Timeout timeout) {
			timeout.bucket = this;
			if (head == null) {
				head = tail = timeout;
			} else {
				tail.next = timeout;
				timeout.prev = tail;
				tail = timeout;
			}
		}

		void remove(Timeout timeout) {
			if (timeout.prev != null) {
				timeout.prev.next = timeout.next;
			}
			if (timeout.next != null) {
				timeout.next.prev = timeout.prev;
			}

			if (timeout == head) {
				if (timeout == tail) {
					head = null;
					tail = null;
				} else {
					head = timeout.next;
				}
			} else if (timeout == tail) {
				tail = timeout.prev;
			}
			timeout.prev = null;
			timeout.next = null;
			timeout.bucket = null;
		}

		void expire(long currentTime) {
			Timeout timeout = head;
			while (timeout != null) {
				Timeout next = timeout.next;
				if (timeout.deadline <= currentTime) {
					timeout.expire();
					remove(timeout);
				}
				timeout = next;
			}
		}
	}
}
