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
 * JDBC
 */
public interface JDBC {
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
		return query(connection, RowMapper.REFLECTIVE(type), sql, args);
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
			ps.execute();
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

	static <T> List<T> queryByParam(Connection connection, T query) throws SQLException {
		return queryByParam(connection, query, null);
	}

	static <T> List<T> queryByParam(Connection connection, T query, String appendix) throws SQLException {
		Map<String, Object> params = getAllParams(query);
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from `" + query.getClass().getSimpleName() + "` where 1 = 1");
		for (String param : params.keySet()) {
			sql.append(" and `" + param + "` = ?");
		}
		if (appendix != null) {
			sql.append(" ").append(appendix);
		}
		return (List<T>) query(connection, query.getClass(), sql.toString(), params.values().toArray());
	}

	static <T> int countByParam(Connection connection, T query) throws SQLException {
		Map<String, Object> params = getAllParams(query);
		StringBuilder sql = new StringBuilder(256);
		sql.append("select count(*) from `" + query.getClass().getSimpleName() + "` where 1 = 1");
		for (String param : params.keySet()) {
			sql.append(" and `" + param + "` = ?");
		}
		return count(connection, sql.toString(), params.values().toArray());
	}

	static <T> long insertData(Connection connection, T data) throws SQLException {
		Map<String, Object> params = getAllParams(data);
		if (params.isEmpty()) {
			return -1;
		}
		StringBuilder sql = new StringBuilder(256);
		sql.append("insert into `" + data.getClass().getSimpleName() + "` (");
		sql.append(params.keySet().stream().map(k -> "`" + k + "`").collect(Collectors.joining(", ")));
		sql.append(") values (");
		List<String> list = new ArrayList<>(params.size());
		for (int i = 0; i < params.size(); i++) {
			list.add("?");
		}
		sql.append(String.join(", ", list));
		sql.append(")");
		return insert(connection, sql.toString(), params.values().toArray());
	}

	static <T> int updateData(Connection connection, T data) throws SQLException {
		Map<String, Object> params = getAllParams(data);
		Object id = params.remove("id");
		if (params.isEmpty()) {
			return -1;
		}
		StringBuilder sql = new StringBuilder(256);
		sql.append("update `" + data.getClass().getSimpleName() + "` set ");
		List<String> list = new ArrayList<>(params.size());
		for (String param : params.keySet()) {
			list.add("`" + param + "` = ?");
		}
		sql.append(String.join(", ", list));
		sql.append(" where `id` = ?");
		List<Object> args = new ArrayList<>(params.values());
		args.add(id);
		return update(connection, sql.toString(), args.toArray());
	}

	static <T> int updateData(Connection connection, T data, T query) throws SQLException {
		Map<String, Object> values = getAllParams(data);
		values.remove("id");
		if (values.isEmpty()) {
			return -1;
		}
		StringBuilder sql = new StringBuilder(256);
		sql.append("update `" + data.getClass().getSimpleName() + "` set ");
		List<String> list = new ArrayList<>(values.size());
		for (String param : values.keySet()) {
			list.add("`" + param + "` = ?");
		}
		sql.append(String.join(", ", list));
		sql.append(" where 1 = 1");
		Map<String, Object> params = getAllParams(query);
		for (String param : params.keySet()) {
			sql.append(" and `" + param + "` = ?");
		}
		return update(connection, sql.toString(), params.values().toArray());
	}

	static <T> int delete(Connection connection, T query) throws SQLException {
		Map<String, Object> params = getAllParams(query);
		if (params.isEmpty()) {
			return -1;
		}
		StringBuilder sql = new StringBuilder(256);
		sql.append("delete from `" + query.getClass().getSimpleName() + "` where 1 = 1");
		for (String param : params.keySet()) {
			sql.append(" and `" + param + "` = ?");
		}
		return update(connection, sql.toString(), params.values().toArray());
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
