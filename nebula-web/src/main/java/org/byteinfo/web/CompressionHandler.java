package org.byteinfo.web;

import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpUtil;
import org.byteinfo.util.misc.Config;

import java.util.HashSet;
import java.util.Set;

public class CompressionHandler extends HttpContentCompressor {
	private static final int minLength;
	private static final Set<String> types = new HashSet<>();

	static {
		minLength = Config.getInt("gzip.minLength");
		for (String type : Config.get("gzip.types").split(",")) {
			types.add(type.strip());
		}
	}

	public CompressionHandler() {
		super(6, 15, 8, minLength);
	}

	@Override
	protected Result beginEncode(HttpResponse headers, String acceptEncoding) throws Exception {
		CharSequence mimeType = HttpUtil.getMimeType(headers);
		if (types.contains(String.valueOf(mimeType))) {
			return super.beginEncode(headers, acceptEncoding);
		} else {
			return null;
		}
	}
}
