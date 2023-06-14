package org.byteinfo.util.misc;

import java.util.Locale;

public enum Platform {
	WINDOWS, LINUX, MAC_OS, UNKNOWN;

	private static final Platform current;

	static {
		String name = System.getProperty("os.name").toLowerCase(Locale.ROOT);
		if (name.contains("windows")) {
			current = WINDOWS;
		} else if (name.contains("linux")) {
			current = LINUX;
		} else if (name.contains("mac os")) {
			current = MAC_OS;
		} else {
			current = UNKNOWN;
		}
	}

	public static Platform currentOS() {
		return current;
	}

	public static boolean isWindows() {
		return current == WINDOWS;
	}

	public static boolean isLinux() {
		return current == LINUX;
	}

	public static boolean isMacOS() {
		return current == MAC_OS;
	}
}
