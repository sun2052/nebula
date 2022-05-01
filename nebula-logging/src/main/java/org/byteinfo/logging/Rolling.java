package org.byteinfo.logging;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Rolling Policy
 */
public class Rolling {
	private final DateTimeFormatter formatter;
	private final ChronoUnit unit;

	/**
	 * No Rolling
	 */
	public static final Rolling NONE = new Rolling(null, null) {
		@Override
		public String getSuffix(long time) {
			return ".log";
		}

		@Override
		public long getNextRollingTime(long time) {
			return Long.MAX_VALUE;
		}
	};

	/**
	 * Hourly, .20160117T08.log
	 */
	public static final Rolling HOURLY = new Rolling("yyyyMMdd'T'HH", ChronoUnit.HOURS);

	/**
	 * Daily, .20160117.log
	 */
	public static final Rolling DAILY = new Rolling("yyyyMMdd", ChronoUnit.DAYS);

	/**
	 * Weekly, .2016W01.log
	 */
	public static final Rolling WEEKLY = new Rolling("yyyy'W'w", ChronoUnit.WEEKS);

	/**
	 * Monthly, .201601.log
	 */
	public static final Rolling MONTHLY = new Rolling("yyyyMM", ChronoUnit.MONTHS);

	/**
	 * Yearly, .2016.log
	 */
	public static final Rolling YEARLY = new Rolling("yyyy", ChronoUnit.YEARS);

	private Rolling(String format, ChronoUnit unit) {
		this.formatter = format == null ? null : DateTimeFormatter.ofPattern("'.'" + format + "'.log'").withZone(ZoneId.systemDefault());
		this.unit = unit;
	}

	/**
	 * Gets log file suffix by time.
	 *
	 * @param time log time in millis
	 * @return suffix for file name, use "" for no suffix
	 */
	public String getSuffix(long time) {
		return formatter.format(Instant.ofEpochMilli(time));
	}

	/**
	 * Gets the next rolling time.
	 *
	 * @param time current time
	 * @return next rolling time
	 */
	public long getNextRollingTime(long time) {
		return Instant.ofEpochMilli(time).truncatedTo(unit).plus(1, unit).toEpochMilli();
	}
}
