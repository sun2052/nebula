package org.byteinfo.web;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.byteinfo.util.io.IOUtil;
import org.byteinfo.util.misc.Config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

public class AssetHandler implements Handler {
	private final Path pathRoot;
	private final String classRoot;
	private final long maxAge;
	private final boolean cacheByETag;

	public AssetHandler() {
		String path = Config.get("asset.pathRoot");
		if (path == null) {
			pathRoot = null;
		} else {
			pathRoot = Path.of(path);
		}
		classRoot = Config.get("asset.classRoot");
		maxAge = Config.getInt("asset.maxAge");
		cacheByETag = Config.getBoolean("asset.eTag");
	}

	@Override
	public void handle(Request request, Response response) throws IOException {
		String path = request.path();
		Asset asset = null;

		if (pathRoot != null) {
			Path file = pathRoot.resolve(path);
			if (Files.exists(file) && Files.isRegularFile(file)) {
				BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
				asset = new Asset(Files.readAllBytes(file), MediaType.byPath(path).orElse(MediaType.OCTETSTREAM), attributes.size(), attributes.lastModifiedTime().toMillis());
			}
		}

		if (asset == null) {
			URL url = IOUtil.getClassResource(classRoot + path);
			if (url == null) {
				throw new WebException("Resource not found: " + path, HttpResponseStatus.NOT_FOUND);
			}
			URLConnection con = url.openConnection();
			con.setUseCaches(false);
			try (InputStream stream = con.getInputStream()) {
				asset = new Asset(stream.readAllBytes(), MediaType.byPath(path).orElse(MediaType.OCTETSTREAM), con.getContentLengthLong(), con.getLastModified());
			}
		}

		response.type(asset.type());

		// handle eTag
		if (cacheByETag) {
			String eTag = asset.eTag();
			response.header(HttpHeaderNames.ETAG, eTag);
			if (eTag.equals(request.headers().get(HttpHeaderNames.IF_NONE_MATCH))) {
				response.status(HttpResponseStatus.NOT_MODIFIED);
				return;
			}
		}

		// cache max-age
		if (maxAge > 0) {
			response.header(HttpHeaderNames.CACHE_CONTROL, "max-age=" + maxAge);
		}

		response.result(asset);
	}
}
