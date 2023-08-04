package org.byteinfo.util.time;

import org.byteinfo.util.function.CheckedConsumer;
import org.byteinfo.util.function.Unchecked;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A handle for the scheduled task.
 */
public class Timeout {
	public static final int ST_INIT = 0;
	public static final int ST_CANCELLED = 1;
	public static final int ST_EXPIRED = 2;

	private volatile int state = ST_INIT;
	private static final AtomicIntegerFieldUpdater<Timeout> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(Timeout.class, "state");

	private final CheckedConsumer<Timeout> task;
	private final long deadline;
	private final WheelTimer timer;
	int pendingRounds;

	WheelTimer.Slot slot;
	Timeout next;
	Timeout prev;

	/**
	 * Creates a new Timeout.
	 *
	 * @param task task to be scheduled
	 * @param deadline task deadline
	 * @param timer timer to be used
	 */
	Timeout(CheckedConsumer<Timeout> task, long deadline, WheelTimer timer) {
		this.task = task;
		this.deadline = deadline;
		this.timer = timer;
	}

	/**
	 * Returns the WheelTimer that created this handle.
	 *
	 * @return current timer
	 */
	public WheelTimer timer() {
		return timer;
	}

	/**
	 * Returns the task which is associated with this handle.
	 *
	 * @return current task
	 */
	public CheckedConsumer<Timeout> task() {
		return task;
	}

	/**
	 * Gets the deadline of this Timeout.
	 *
	 * @return deadline in millis
	 */
	public long deadline() {
		return deadline;
	}

	/**
	 * Gets the state of this Timeout.
	 *
	 * @return timeout state
	 */
	public int state() {
		return state;
	}

	/**
	 * Cancels this Timeout.
	 *
	 * @return true if successful
	 */
	public boolean cancel() {
		if (compareAndSetState(ST_INIT, ST_CANCELLED)) {
			return timer.cancelled.add(this);
		}
		return false;
	}

	/**
	 * Executes this Timeout.
	 */
	public void execute(ExecutorService executor) {
		if (compareAndSetState(ST_INIT, ST_EXPIRED)) {
			executor.execute(Unchecked.runnable(() -> task.accept(this)));
		}
	}

	/**
	 * Atomically updates the state of this Timeout.
	 *
	 * @param expected the expected state
	 * @param state the new state
	 * @return true if successful
	 */
	public boolean compareAndSetState(int expected, int state) {
		return STATE_UPDATER.compareAndSet(this, expected, state);
	}

	@Override
	public String toString() {
		return "Timeout{state=%d, deadline=%d, pendingRounds=%d}".formatted(state, deadline, pendingRounds);
	}
}
