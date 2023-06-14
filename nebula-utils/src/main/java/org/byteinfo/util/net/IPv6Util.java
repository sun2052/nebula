package org.byteinfo.util.net;

interface IPv6Util {
	static boolean isPrivate(String address) {
		boolean isPrivateIPv6 = false;
		String[] parts = address.trim().split(":");
		if (parts.length > 0) {
			String firstBlock = parts[0];
			String prefix = firstBlock.substring(0, 2);
			if (firstBlock.equalsIgnoreCase("fe80")
					|| firstBlock.equalsIgnoreCase("100")
					|| ((prefix.equalsIgnoreCase("fc") && firstBlock.length() >= 4))
					|| ((prefix.equalsIgnoreCase("fd") && firstBlock.length() >= 4))) {
				isPrivateIPv6 = true;
			}
		}
		return isPrivateIPv6;
	}
}
