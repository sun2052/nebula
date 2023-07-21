package org.byteinfo.util.io;

public interface FileUtil {
	static String getName(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return fileName;
		}
		int index = indexOfLastSeparator(fileName);
		return fileName.substring(index + 1);
	}

	static String getPath(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return fileName;
		}
		int index = indexOfLastSeparator(fileName);
		return fileName.substring(0, index);
	}

	static String getBaseName(String fileName) {
		return removeExtension(getName(fileName));
	}

	static String getExtension(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return fileName;
		}
		int index = fileName.lastIndexOf('.');
		if (index == -1) {
			return "";
		}
		return fileName.substring(index + 1);
	}

	static String removeExtension(String fileName) {
		if (fileName == null || fileName.isEmpty()) {
			return fileName;
		}
		int index = fileName.lastIndexOf('.');
		if (index == -1) {
			return fileName;
		}
		return fileName.substring(0, index);
	}

	private static int indexOfLastSeparator(String fileName) {
		if (fileName == null) {
			return -1;
		}
		return Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
	}
}
