package org.byteinfo.util.text;

import java.text.DecimalFormat;

public interface SizeUtil {
	static String toHumanReadable(long bytes) {
		String[] units = {"B", "KB", "MB", "GB", "TB", "PB", "EB", "ZB", "YB"};
		double size = bytes;
		int i = 0;
		while (size >= 1024) {
			size /= 1024;
			i++;
		}
		return new DecimalFormat("#.##").format(size) + " " + units[i];
	}
}
