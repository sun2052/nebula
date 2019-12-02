package org.byteinfo.util.time;

import org.byteinfo.util.function.CheckedConsumer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A WheelTimer optimized for approximated I/O timeout scheduling.
 */
public class WheelTimer {
	private final Bucket[] wheel;
	private final long precision;
	private final int mask;
	final Queue<Timeout> pending = new ConcurrentLinkedQueue<>();
	final Queue<Timeout> cancelled = new ConcurrentLinkedQueue<>();
	final CheckedConsumer<Exception> exceptionHandler;
	private volatile boolean started = true;

	public WheelTimer() {
		this(100, 1024);
	}

	public WheelTimer(int precision, int wheelSize) {
		this(precision, wheelSize, e -> {});
	}

	/**
	 * Creates a new timer.
	 *
	 * @param precision precision in millis
	 * @param wheelSize the size of the wheel
	 * @param exceptionHandler exception handler for scheduled task
	 */
	public WheelTimer(int precision, int wheelSize, CheckedConsumer<Exception> exceptionHandler) {
		if (precision <= 0) {
			throw new IllegalArgumentException(String.format("precision: %d (expected: precision > 0)", precision));
		}
		this.precision = precision;
		if (wheelSize <= 0 || wheelSize > 1 << 30) {
			throw new IllegalArgumentException(String.format("wheelSize: %d (expected: 0 < wheelSize <= %d)", wheelSize, 1 << 30));
		}
		int size = 1;
		while (size < wheelSize) {
			size <<= 1;
		}
		mask = size - 1;
		wheel = new Bucket[size];
		for (int i = 0; i < size; i++) {
			wheel[i] = new Bucket();
		}
		this.exceptionHandler = exceptionHandler;

		Thread worker = new Thread(() -> {
			int tick = 0;
			while (started) {
				long currentTime = System.currentTimeMillis();
				Timeout timeout;

				while ((timeout = pending.poll()) != null) {
					if (timeout.state() == Timeout.ST_CANCELLED) {
						continue;
					}
					if (timeout.deadline <= currentTime) {
						timeout.execute();
					} else {
						int ticks = (int) ((timeout.deadline - currentTime) / precision);
						timeout.pendingRounds = ticks / wheel.length;
						wheel[(ticks + tick) & mask].add(timeout);
					}
				}

				wheel[tick].execute();

				while ((timeout = cancelled.poll()) != null) {
					if (timeout.bucket != null) {
						timeout.bucket.remove(timeout);
					}
				}

				long time = precision - (System.currentTimeMillis() - currentTime);
				if (time > 0) {
					try {
						Thread.sleep(time);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}

				tick++;
				if (tick == wheelSize) {
					tick -= wheelSize;
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
		if (!started) {
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
		started = false;
	}

	/**
	 * Gets the default WheelTimer.
	 *
	 * @return WheelTimer
	 */
	public static WheelTimer getDefault() {
		return Holder.TIMER;
	}

	@Override
	public String toString() {
		return "WheelTimer{" + "precision=" + precision + "ms, wheelSize=" + wheel.length + ", started=" + started + '}';
	}

	private static class Holder {
		private static final WheelTimer TIMER = new WheelTimer();
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

		void execute() {
			Timeout timeout = head;
			while (timeout != null) {
				Timeout next = timeout.next;
				if (timeout.pendingRounds == 0) {
					timeout.execute();
					remove(timeout);
				} else {
					timeout.pendingRounds--;
				}
				timeout = next;
			}
		}
	}
}
