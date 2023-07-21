package org.byteinfo.util.time;

import org.byteinfo.util.function.CheckedConsumer;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An efficient Timer for approximated timeout scheduling.
 *
 * @implSpec This class is thread-safe.
 */
public final class WheelTimer implements AutoCloseable {
	private final long precision;
	private final Slot[] wheel;

	private volatile boolean started = true;

	final Queue<Timeout> pending = new ConcurrentLinkedQueue<>();
	final Queue<Timeout> cancelled = new ConcurrentLinkedQueue<>();

	/**
	 * Creates a new WheelTimer.
	 */
	public WheelTimer() {
		this(1024, 100);
	}

	/**
	 * Creates a new WheelTimer.
	 *
	 * @param wheelSize the size of the wheel, 0 < wheelSize <= 10_7374_1824 (1 << 30）
	 * @param precision precision of the timer in millis, precision > 0
	 */
	public WheelTimer(int wheelSize, int precision) {
		this(Executors.newVirtualThreadPerTaskExecutor(), wheelSize, precision);
	}

	/**
	 * Creates a new WheelTimer.
	 *
	 * @param executor executor for the scheduled task
	 * @param wheelSize the size of the wheel, 0 < wheelSize <= 10_7374_1824 (1 << 30）
	 * @param precision precision of the timer in millis, precision > 0
	 */
	public WheelTimer(ExecutorService executor, int wheelSize, int precision) {
		Objects.requireNonNull(executor, "executor");
		if (wheelSize <= 0 || wheelSize > 1 << 30) {
			throw new IllegalArgumentException(String.format("wheelSize: %d (expected: 0 < wheelSize <= %d)", wheelSize, 1 << 30));
		}
		if (precision <= 0) {
			throw new IllegalArgumentException(String.format("precision: %d (expected: precision > 0)", precision));
		}
		this.precision = precision;

		int size = 1;
		while (size < wheelSize) {
			size <<= 1;
		}
		wheel = new Slot[size];
		for (int i = 0; i < size; i++) {
			wheel[i] = new Slot();
		}

		Thread.ofVirtual().name(String.format("%s-%d-%d", getClass().getSimpleName(), size, precision)).start(() -> {
			int mask = wheel.length - 1; // y = Math.pow(2, n); x % y == x & (y - 1)
			int tick = 0;
			long diff = 0;
			while (started) {
				// process current tick
				long startTime = System.currentTimeMillis();
				Timeout timeout;

				// process all newly scheduled tasks
				while ((timeout = pending.poll()) != null) {
					if (timeout.state() == Timeout.ST_CANCELLED) {
						continue;
					}
					if (timeout.deadline() <= startTime) {
						timeout.execute(executor);
					} else {
						int ticks = (int) Math.round((timeout.deadline() - startTime) * 1.0 / precision);
						int index = (ticks + tick) & mask;
						wheel[index].add(timeout);
						timeout.pendingRounds = ticks / wheel.length;
					}
				}

				// execute all tasks in current slot
				wheel[tick].execute(executor);

				// remove all cancelled tasks
				while ((timeout = cancelled.poll()) != null) {
					if (timeout.slot != null) {
						timeout.slot.remove(timeout);
					}
				}

				// wait for next tick
				long nextStartTime = startTime + precision + diff;
				long sleep = nextStartTime - System.currentTimeMillis();
				if (sleep > 0) {
					try {
						Thread.sleep(sleep);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}

				// move to next slot
				tick++;
				if (tick == wheelSize) {
					tick -= wheelSize;
				}

				// try to fix the deviation caused by Thread.sleep()
				diff = nextStartTime - System.currentTimeMillis();
			}
		});
	}

	/**
	 * Schedules the execution of the task after the delay.
	 *
	 * @param task task to be executed
	 * @param delay delay in millis
	 * @return a handle for the scheduled task
	 */
	public Timeout newTimeout(CheckedConsumer<Timeout> task, long delay) {
		if (!started) {
			throw new IllegalStateException("WheelTimer has been closed.");
		}
		long deadline = System.currentTimeMillis() + delay;
		Timeout timeout = new Timeout(task, deadline, this);
		pending.add(timeout);
		return timeout;
	}

	/**
	 * Closes the WheelTimer and ignores all pending tasks.
	 */
	public void close() {
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
		return String.format("WheelTimer{wheelSize=%d, precision=%d, started=%s}", wheel.length, precision, started);
	}

	private static class Holder {
		private static final WheelTimer TIMER = new WheelTimer();
	}

	// each slot in the wheel is a doubly linked list of timeout
	static class Slot {
		Timeout head;
		Timeout tail;

		void add(Timeout timeout) {
			timeout.slot = this;
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
			timeout.slot = null;
		}

		void execute(ExecutorService executor) {
			Timeout timeout = head;
			while (timeout != null) {
				Timeout next = timeout.next;
				if (timeout.pendingRounds == 0) {
					timeout.execute(executor);
					remove(timeout);
				} else {
					timeout.pendingRounds--;
				}
				timeout = next;
			}
		}
	}
}
