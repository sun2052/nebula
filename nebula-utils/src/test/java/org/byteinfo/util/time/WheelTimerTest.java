package org.byteinfo.util.time;

import org.byteinfo.util.function.Unchecked;

public class WheelTimerTest {
	public static void main(String[] args) {
		var timer = WheelTimer.getDefault();
		for (int i = 0; i < 100; i++) {
			timer.newTimeout(t -> {
				long start = System.currentTimeMillis();
				System.out.printf("deadline=%d, executed=%d, delay=%d%n", t.deadline(), start, start - t.deadline());
			}, i * 100L);
		}

		Unchecked.runnable(() -> Thread.sleep(100 * 100)).run();
	}
}