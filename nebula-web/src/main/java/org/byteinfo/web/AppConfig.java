package org.byteinfo.web;

import org.byteinfo.util.misc.Config;

public class AppConfig {
	private static final Config config = new Config();

	public static Config get() {
		return config;
	}
}
