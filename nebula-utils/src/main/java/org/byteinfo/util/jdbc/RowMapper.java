package org.byteinfo.util.jdbc;

import org.byteinfo.util.function.Unchecked;
import org.byteinfo.util.reflect.Reflect;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.sql.Blob;
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
	 * @throws SQLException if a SQLException is encountered getting column values
	 */
	T mapRow(ResultSet rs, int rowNum) throws SQLException;

	/**
	 * Get a reflective RowMapper of the specified type.
	 *
	 * @param clazz target type
	 * @return reflective RowMapper
	 */
	static <T> RowMapper<T> reflective(Class<T> clazz) {
		return (rs, rowNum) -> {
			try {
				ResultSetMetaData meta = rs.getMetaData();
				if (meta.getColumnCount() == 1) {
					return clazz.cast(getObject(clazz, rs, 1));
				} else {
					Map<String, Integer> map = new HashMap<>();
					for (int i = 1; i <= meta.getColumnCount(); i++) {
						map.put(meta.getColumnLabel(i), i);
					}
					return Reflect.create(clazz, Unchecked.biFunction((name, type) -> getObject(type, rs, map.get(name))));
				}
			} catch (Exception e) {
				throw new SQLException(e);
			}
		};
	}

	private static Object getObject(Type type, ResultSet rs, int index) throws SQLException, IOException {
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
		} else if (type == InputStream.class) {
			Blob blob = rs.getBlob(index);
			return blob == null ? null : blob.getBinaryStream();
		} else if (type == byte[].class) {
			Blob blob = rs.getBlob(index);
			return blob == null ? null : blob.getBinaryStream().readAllBytes();
		}
		throw new UnsupportedOperationException("Unsupported type: " + type + ", Supported: String, Integer, Long, Boolean, LocalDate, LocalTime, LocalDateTime, InputStream, byte[]");
	}
}
