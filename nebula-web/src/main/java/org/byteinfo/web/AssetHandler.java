package org.byteinfo.web;

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

	public AssetHandler() {
		String path = Config.get("asset.pathRoot");
		if (path == null) {
			pathRoot = null;
		} else {
			pathRoot = Path.of(path);
		}
		classRoot = Config.get("asset.classRoot");
	}

	@Override
	public Object handle(HttpContext context) throws IOException {
		String path = context.path();

		if (pathRoot != null) {
			Path file = pathRoot.resolve(path);
			if (Files.exists(file) && Files.isRegularFile(file)) {
				BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
				return new Asset(Files.readAllBytes(file), MediaType.byPath(path).orElse(MediaType.OCTETSTREAM), attributes.size(), attributes.lastModifiedTime().toMillis());
			}
		}

		URL url = IOUtil.getClassResource(classRoot + path);
		if (url == null) {
			throw new WebException("Resource not found: " + path, HttpResponseStatus.NOT_FOUND);
		}
		URLConnection con = url.openConnection();
		con.setUseCaches(false);
		try (InputStream stream = con.getInputStream()) {
			return new Asset(stream.readAllBytes(), MediaType.byPath(path).orElse(MediaType.OCTETSTREAM), con.getContentLengthLong(), con.getLastModified());
		}
	}
}
