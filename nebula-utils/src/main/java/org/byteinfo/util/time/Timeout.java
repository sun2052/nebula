package org.byteinfo.util.time;

import org.byteinfo.util.function.CheckedConsumer;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A handle associated with a task that is returned by a WheelTimer.
 */
public class Timeout {
	public static final int ST_INIT = 0;
	public static final int ST_CANCELLED = 1;
	public static final int ST_EXPIRED = 2;

	final CheckedConsumer<Timeout> task;
	final WheelTimer timer;
	final long deadline;
	private volatile int state = ST_INIT;
	private static final AtomicIntegerFieldUpdater<Timeout> STATE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(Timeout.class, "state");

	WheelTimer.Bucket bucket;
	Timeout next;
	Timeout prev;

	/**
	 * Creates a new Timeout.
	 *
	 * @param task task to be scheduled
	 * @param timer timer to be used
	 * @param deadline task deadline
	 */
	Timeout(CheckedConsumer<Timeout> task, WheelTimer timer, long deadline) {
		this.task = task;
		this.timer = timer;
		this.deadline = deadline;
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
	public void expire() {
		if (compareAndSetState(ST_INIT, ST_EXPIRED)) {
			try {
				task.accept(this);
			} catch (Exception e) {
				try {
					timer.exceptionHandler.accept(e);
				} catch (Exception ex) {
					// ignore
				}
			}
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
		return "Timeout{" + "timer=" + timer + ", deadline=" + deadline + ", state=" + state + '}';
	}
}
