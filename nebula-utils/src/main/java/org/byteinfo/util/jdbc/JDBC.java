package org.byteinfo.util.jdbc;

import org.byteinfo.util.reflect.Reflect;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JDBC Util
 */
public interface JDBC {

	/* ---------------- Universal Methods -------------- */

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of arguments to bind to the query, mapping each row to a Java object via a Reflective RowMapper.
	 *
	 * @param connection the database Connection
	 * @param type mapped object type
	 * @param sql SQL query to execute
	 * @param args arguments to bind to the query
	 * @return the result List, containing mapped objects
	 * @throws SQLException if the query fails
	 */
	static <T> List<T> query(Connection connection, Class<T> type, String sql, Object... args) throws SQLException {
		return query(connection, RowMapper.reflective(type), sql, args);
	}

	/**
	 * Query given SQL to create a prepared statement from SQL and a list of arguments to bind to the query, mapping each row to a Java object via a RowMapper.
	 *
	 * @param connection the database Connection
	 * @param rowMapper object that will map one object per row
	 * @param sql SQL query to execute
	 * @param args arguments to bind to the query
	 * @return the result List, containing mapped objects
	 * @throws SQLException if the query fails
	 */
	static <T> List<T> query(Connection connection, RowMapper<T> rowMapper, String sql, Object... args) throws SQLException {
		List<T> list = new ArrayList<>();
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			int index = 1;
			for (Object arg : args) {
				ps.setObject(index++, arg);
			}
			ResultSet rs = ps.executeQuery();
			index = 1;
			while (rs.next()) {
				T obj = rowMapper.mapRow(rs, index++);
				if (obj != null) {
					list.add(obj);
				}
			}
		}
		return list;
	}

	/**
	 * Query the matched count by the given SQL and a list of arguments.
	 *
	 * @param connection the JDBC Connection
	 * @param sql the SQL statement to execute
	 * @param args arguments to bind to the query
	 * @return the matched count
	 * @throws SQLException if an error occurs
	 */
	static int count(Connection connection, String sql, Object... args) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			int index = 1;
			for (Object arg : args) {
				ps.setObject(index++, arg);
			}
			ResultSet rs = ps.executeQuery();
			rs.next();
			return rs.getInt(1);
		}
	}

	/**
	 * Issue a single SQL insert operation via a prepared statement, binding the given arguments.
	 *
	 * @param connection the JDBC Connection
	 * @param sql the SQL statement to execute
	 * @param args arguments to bind to the query
	 * @return the generated key or -1 if not exists
	 * @throws SQLException if an error occurs
	 */
	static long insert(Connection connection, String sql, Object... args) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			int index = 1;
			for (Object arg : args) {
				ps.setObject(index++, arg);
			}
			ps.executeUpdate();
			ResultSet rs = ps.getGeneratedKeys();
			return rs.next() ? rs.getLong(1) : -1;
		}
	}

	/**
	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
	 *
	 * @param connection the JDBC Connection
	 * @param sql the SQL statement to execute
	 * @param argsList the Array of Object arrays containing the batch of arguments for the query
	 * @return an array containing the generated keys by each insert in the batch
	 * @throws SQLException if an error occurs
	 */
	static long[] batchInsert(Connection connection, String sql, Object[]... argsList) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
			addBatch(ps, argsList);
			ps.executeBatch();
			ResultSet rs = ps.getGeneratedKeys();
			long[] keys = new long[argsList.length];
			int index = 0;
			while (rs.next()) {
				keys[index++] = rs.getLong(1);
			}
			return keys;
		}
	}

	/**
	 * Issue a single SQL update operation (such as an insert, update or delete statement) via a prepared statement, binding the given arguments.
	 *
	 * @param connection database connection
	 * @param sql SQL containing bind parameters
	 * @param args arguments to bind to the query, null if not needed
	 * @return the number of rows affected
	 * @throws SQLException if the update fails
	 */
	static int update(Connection connection, String sql, Object... args) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			int index = 1;
			for (Object arg : args) {
				ps.setObject(index++, arg);
			}
			return ps.executeUpdate();
		}
	}

	/**
	 * Execute a batch using the supplied SQL statement with the batch of supplied arguments.
	 *
	 * @param connection database connection
	 * @param sql the SQL statement to execute
	 * @param argsList the Array of Object arrays containing the batch of arguments for the query
	 * @return an array containing the numbers of rows affected by each update in the batch
	 * @throws SQLException if the update fails
	 */
	static int[] batchUpdate(Connection connection, String sql, Object[]... argsList) throws SQLException {
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			addBatch(ps, argsList);
			return ps.executeBatch();
		}
	}


	/* ---------------- Convenience Methods -------------- */

	static <T> List<T> selectList(Connection connection, T query) throws SQLException {
		return selectList(connection, query, null, -1, -1);
	}

	static <T> List<T> selectList(Connection connection, T query, int limit, int offset) throws SQLException {
		return selectList(connection, query, null, limit, offset);
	}

	static <T> List<T> selectList(Connection connection, T query, String appendix, int limit, int offset) throws SQLException {
		Objects.requireNonNull(query);
		String columns = Reflect.getInstanceFields(query.getClass()).keySet().stream().map(name -> "`" + name + "`").collect(Collectors.joining(", "));
		if (columns.isEmpty()) {
			return List.of();
		}
		StringBuilder sql = new StringBuilder(256);
		sql.append("select ").append(columns).append(" from `").append(query.getClass().getSimpleName()).append("` where 1 = 1");
		List<Object> args = new ArrayList<>();
		getAllParams(query).forEach((key, value) -> {
			sql.append(" and `").append(key).append("` = ?");
			args.add(value);
		});
		if (appendix != null) {
			sql.append(" ").append(appendix);
		}
		if (limit > 0) {
			sql.append(" limit ").append(limit);
		}
		if (offset > 0) {
			sql.append(" offset ").append(offset);
		}
		return (List<T>) query(connection, query.getClass(), sql.toString(), args.toArray());
	}

	static <T> int selectCount(Connection connection, T query) throws SQLException {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select count(*) from `").append(query.getClass().getSimpleName()).append("` where 1 = 1");
		List<Object> args = new ArrayList<>();
		getAllParams(query).forEach((key, value) -> {
			sql.append(" and `").append(key).append("` = ?");
			args.add(value);
		});
		return count(connection, sql.toString(), args.toArray());
	}

	static <T> long insertData(Connection connection, T data) throws SQLException {
		Map<String, Object> params = getAllParams(data);
		if (params.isEmpty()) {
			return -1;
		}
		List<String> columns = new ArrayList<>();
		List<String> placeholders = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		params.forEach((key, value) -> {
			columns.add("`" + key + "`");
			placeholders.add("?");
			args.add(value);
		});
		String sql = "insert into `" + data.getClass().getSimpleName() + "` (" + String.join(", ", columns) + ") values (" + String.join(", ", placeholders) + ")";
		return insert(connection, sql, args.toArray());
	}

	static <T> int updateData(Connection connection, T data) throws SQLException {
		return updateData(connection, data, null);
	}

	static <T> int updateData(Connection connection, T data, T query) throws SQLException {
		Map<String, Object> params = getAllParams(data);
		Object id = params.remove("id");
		if (params.isEmpty()) {
			return -1;
		}
		List<String> columns = new ArrayList<>();
		List<Object> args = new ArrayList<>();
		params.forEach((key, value) -> {
			columns.add("`" + key + "` = ?");
			args.add(value);
		});
		StringBuilder sql = new StringBuilder(256);
		sql.append("update `").append(data.getClass().getSimpleName()).append("` set ");
		sql.append(String.join(", ", columns));
		if (query != null) {
			Map<String, Object> conditions = getAllParams(query);
			if (conditions.isEmpty()) {
				query = null;
			} else {
				sql.append(" where 1 = 1");
				conditions.forEach((key, value) -> {
					sql.append(" and `").append(key).append("` = ?");
					args.add(value);
				});
			}
		}
		if (query == null) {
			sql.append(" where `id` = ?");
			args.add(id);
		}
		return update(connection, sql.toString(), args.toArray());
	}

	static <T> int deleteData(Connection connection, T query) throws SQLException {
		Map<String, Object> params = getAllParams(query);
		if (params.isEmpty()) {
			return -1;
		}
		StringBuilder sql = new StringBuilder(256);
		sql.append("delete from `").append(query.getClass().getSimpleName()).append("` where 1 = 1");
		List<Object> args = new ArrayList<>();
		params.forEach((key, value) -> {
			sql.append(" and `").append(key).append("` = ?");
			args.add(value);
		});
		return update(connection, sql.toString(), args.toArray());
	}

	private static Map<String, Object> getAllParams(Object query) throws SQLException {
		Objects.requireNonNull(query);
		Map<String, Object> map = new HashMap<>();
		Map<String, Field> fields = Reflect.getInstanceFields(query.getClass());
		for (Field field : fields.values()) {
			try {
				Object value = field.get(query);
				if (value instanceof Optional<?> optional) {
					value = optional.orElse(null);
				}
				if (value != null) {
					map.putIfAbsent(field.getName(), value);
				}
			} catch (IllegalAccessException e) {
				throw new SQLException(e);
			}
		}
		return map;
	}

	private static void addBatch(PreparedStatement ps, Object[]... argsList) throws SQLException {
		for (Object[] args : argsList) {
			int index = 1;
			for (Object arg : args) {
				ps.setObject(index++, arg);
			}
			ps.addBatch();
		}
	}
}
