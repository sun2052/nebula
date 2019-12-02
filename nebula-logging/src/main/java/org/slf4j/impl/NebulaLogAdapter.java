package org.slf4j.impl;

import org.byteinfo.logging.Level;
import org.byteinfo.logging.Log;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

public final class NebulaLogAdapter extends MarkerIgnoringBase {
	private static final Object[] EMPTY_OBJECT_ARRAY = new Object[] {};

	@Override
	public boolean isTraceEnabled() {
		return Log.isLoggable(Level.TRACE);
	}

	@Override
	public void trace(String msg) {
		Log.dispatch(Level.TRACE, msg, EMPTY_OBJECT_ARRAY, null, null);
	}

	@Override
	public void trace(String format, Object arg) {
		if (isTraceEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg);
			Log.dispatch(Level.TRACE, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void trace(String format, Object arg1, Object arg2) {
		if (isTraceEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
			Log.dispatch(Level.TRACE, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void trace(String format, Object... arguments) {
		if (isTraceEnabled()) {
			FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
			Log.dispatch(Level.TRACE, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void trace(String msg, Throwable t) {
		Log.dispatch(Level.TRACE, msg, EMPTY_OBJECT_ARRAY, null, t);
	}

	@Override
	public boolean isDebugEnabled() {
		return Log.isLoggable(Level.DEBUG);
	}

	@Override
	public void debug(String msg) {
		Log.dispatch(Level.DEBUG, msg, EMPTY_OBJECT_ARRAY, null, null);
	}

	@Override
	public void debug(String format, Object arg) {
		if (isDebugEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg);
			Log.dispatch(Level.DEBUG, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void debug(String format, Object arg1, Object arg2) {
		if (isDebugEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
			Log.dispatch(Level.DEBUG, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void debug(String format, Object... arguments) {
		if (isDebugEnabled()) {
			FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
			Log.dispatch(Level.DEBUG, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void debug(String msg, Throwable t) {
		Log.dispatch(Level.DEBUG, msg, EMPTY_OBJECT_ARRAY, null, t);
	}

	@Override
	public boolean isInfoEnabled() {
		return Log.isLoggable(Level.INFO);
	}

	@Override
	public void info(String msg) {
		Log.dispatch(Level.INFO, msg, EMPTY_OBJECT_ARRAY, null, null);
	}

	@Override
	public void info(String format, Object arg) {
		if (isInfoEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg);
			Log.dispatch(Level.INFO, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void info(String format, Object arg1, Object arg2) {
		if (isInfoEnabled()) {

			FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
			Log.dispatch(Level.INFO, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void info(String format, Object... arguments) {
		if (isInfoEnabled()) {
			FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
			Log.dispatch(Level.INFO, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void info(String msg, Throwable t) {
		Log.dispatch(Level.INFO, msg, EMPTY_OBJECT_ARRAY, null, t);
	}

	@Override
	public boolean isWarnEnabled() {
		return Log.isLoggable(Level.WARN);
	}

	@Override
	public void warn(String msg) {
		Log.dispatch(Level.WARN, msg, EMPTY_OBJECT_ARRAY, null, null);
	}

	@Override
	public void warn(String format, Object arg) {
		if (isWarnEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg);
			Log.dispatch(Level.WARN, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void warn(String format, Object arg1, Object arg2) {
		if (isWarnEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
			Log.dispatch(Level.WARN, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void warn(String format, Object... arguments) {
		if (isWarnEnabled()) {
			FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
			Log.dispatch(Level.WARN, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void warn(String msg, Throwable t) {
		Log.dispatch(Level.WARN, msg, EMPTY_OBJECT_ARRAY, null, t);
	}

	@Override
	public boolean isErrorEnabled() {
		return Log.isLoggable(Level.ERROR);
	}

	@Override
	public void error(String msg) {
		Log.dispatch(Level.ERROR, msg, EMPTY_OBJECT_ARRAY, null, null);
	}

	@Override
	public void error(String format, Object arg) {
		if (isErrorEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg);
			Log.dispatch(Level.ERROR, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void error(String format, Object arg1, Object arg2) {
		if (isErrorEnabled()) {
			FormattingTuple ft = MessageFormatter.format(format, arg1, arg2);
			Log.dispatch(Level.ERROR, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void error(String format, Object... arguments) {
		if (isErrorEnabled()) {
			FormattingTuple ft = MessageFormatter.arrayFormat(format, arguments);
			Log.dispatch(Level.ERROR, ft.getMessage(), EMPTY_OBJECT_ARRAY, null, ft.getThrowable());
		}
	}

	@Override
	public void error(String msg, Throwable t) {
		Log.dispatch(Level.ERROR, msg, EMPTY_OBJECT_ARRAY, null, t);
	}
}
