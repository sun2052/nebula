package org.byteinfo.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class Rolling {
	/**
	 * No Rolling
	 */
	public static final Rolling NONE = new Rolling();

	/**
	 * Daily, 20160117
	 */
	public static final Rolling DAILY = new Rolling() {
		private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneId.systemDefault());

		@Override
		public String getRollingSuffix(long millis) {
			return FORMATTER.format(Instant.ofEpochMilli(millis));
		}

		@Override
		public long getNextRollingTime(long millis) {
			return Instant.ofEpochMilli(millis).plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS).toEpochMilli();

		}
	};

	/**
	 * Monthly, 201601
	 */
	public static final Rolling MONTHLY = new Rolling() {
		private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM").withZone(ZoneId.systemDefault());

		@Override
		public String getRollingSuffix(long millis) {
			return FORMATTER.format(Instant.ofEpochMilli(millis));
		}

		@Override
		public long getNextRollingTime(long millis) {
			return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).with(TemporalAdjusters.firstDayOfNextMonth()).truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli();
		}
	};

	public String getRollingSuffix(long millis) {
		return "";
	}

	public long getNextRollingTime(long millis) {
		return Long.MAX_VALUE;
	}
}
