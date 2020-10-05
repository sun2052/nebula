package org.byteinfo.util.jdbc;

import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

/**
 * RowMapper
 */
@FunctionalInterface
public interface RowMapper<T> {
	/**
	 * Implementations must implement this method to map each row of data in the ResultSet.
	 * This method should not call next() on the ResultSet; it is only supposed to map values of the current row.
	 *
	 * @param rs the ResultSet to map (pre-initialized for the current row)
	 * @param rowNum the number of the current row; starting at 1
	 * @return the result object for the current row (may be null)
	 * @throws SQLException if a SQLException is encountered getting column values (that is, there's no need to catch SQLException)
	 */
	T mapRow(ResultSet rs, int rowNum) throws SQLException;

	/**
	 * Get a reflective RowMapper of the specified type.
	 *
	 * @param type target type
	 * @return reflective RowMapper
	 */
	static <T> RowMapper<T> REFLECTIVE(Class<T> type) {
		return (rs, rowNum) -> {
			try {
				ResultSetMetaData meta = rs.getMetaData();
				if (meta.getColumnCount() == 1) {
					return type.cast(getObject(type, rs, 1));
				} else {
					// get all fields in the hierarchy
					Map<String, Field> map = new HashMap<>();
					Class<?> current = type;
					while (current.getSuperclass() != null) {
						for (Field field : current.getDeclaredFields()) {
							map.putIfAbsent(field.getName(), field);
						}
						current = current.getSuperclass();
					}

					// map column to field using ColumnLabel
					T target = type.getConstructor().newInstance();
					for (int i = 1; i <= meta.getColumnCount(); i++) {
						String name = meta.getColumnLabel(i);
						Field field = map.get(name);
						if (field != null) {
							field.setAccessible(true);
							field.set(target, getObject(field.getType(), rs, i));
						}
					}
					return target;
				}
			} catch (ReflectiveOperationException e) {
				throw new SQLException(e);
			}
		};
	}

	private static Object getObject(Class<?> type, ResultSet rs, int index) throws SQLException {
		if (type == String.class) {
			return rs.getString(index);
		} else if (type == Integer.class) {
			return rs.getInt(index);
		} else if (type == Long.class) {
			return rs.getLong(index);
		} else if (type == Boolean.class) {
			return rs.getBoolean(index);
		} else if (type == LocalDate.class) {
			Date date = rs.getDate(index);
			return date == null ? null : date.toLocalDate();
		} else if (type == LocalTime.class) {
			Time time = rs.getTime(index);
			return time == null ? null : time.toLocalTime();
		} else if (type == LocalDateTime.class) {
			Timestamp timestamp = rs.getTimestamp(index);
			return timestamp == null ? null : timestamp.toLocalDateTime();
		}
		throw new UnsupportedOperationException("unsupported type: " + type + ", supported: String, Integer, Long, Boolean, LocalDate, LocalTime, LocalDateTime");
	}
}
