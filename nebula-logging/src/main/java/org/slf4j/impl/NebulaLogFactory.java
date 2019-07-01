package org.slf4j.impl;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public class NebulaLogFactory implements ILoggerFactory {
	private static final NebulaLogAdapter NEBULA_LOG_ADAPTER = new NebulaLogAdapter();

	@Override
	public Logger getLogger(String name) {
		return NEBULA_LOG_ADAPTER;
	}
}
