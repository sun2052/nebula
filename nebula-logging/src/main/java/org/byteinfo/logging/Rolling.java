package org.byteinfo.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Rolling {
	private final DateTimeFormatter formatter;
	private final ChronoUnit unit;

	/**
	 * No Rolling
	 */
	public static final Rolling NONE = new Rolling(null, null) {
		@Override
		public String getRollingSuffix(long time) {
			return "";
		}

		@Override
		public long getNextRollingTime(long time) {
			return Long.MAX_VALUE;
		}
	};

	/**
	 * Daily, 20160117
	 */
	public static final Rolling DAILY = new Rolling("yyyyMMdd", ChronoUnit.DAYS);

	/**
	 * Monthly, 201601
	 */
	public static final Rolling MONTHLY = new Rolling("yyyyMM", ChronoUnit.MONTHS);

	private Rolling(String format, ChronoUnit unit) {
		this.formatter = format == null ? null : DateTimeFormatter.ofPattern(format).withZone(ZoneId.systemDefault());
		this.unit = unit;
	}

	public String getRollingSuffix(long time) {
		return formatter.format(Instant.ofEpochMilli(time));
	}

	public long getNextRollingTime(long time) {
		return Instant.ofEpochMilli(time).truncatedTo(unit).plus(1, unit).toEpochMilli();
	}
}
