package org.byteinfo.util.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
	 * Issue a single SQL insert operation via a prepared statement, binding the given arguments.
	 *
	 * @param connection the JDBC Connection
	 * @param sql the SQL statement to execute
	 * @param args arguments to bind to the query
	 * @return the generated key
	 * @throws SQLException if an error occurs
	 */
	static long insert(Connection connection, String sql, Object... args) throws SQLException {
		return batchInsert(connection, sql, args)[0];
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
		return batchUpdate(connection, sql, args)[0];
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

	private static void addBatch(PreparedStatement ps, Object[]... argsList) throws SQLException {
		int index;
		for (Object[] args : argsList) {
			index = 1;
			for (Object arg : args) {
				ps.setObject(index++, arg);
			}
			ps.addBatch();
		}
	}

}
