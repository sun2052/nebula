package org.byteinfo.web;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class AssetHandler implements Handler {
	private final Path fileRoot;
	private final String classRoot;

	public AssetHandler() {
		String root = AppConfig.get().get("asset.fileRoot");
		fileRoot = root == null ? null : Path.of(root);
		classRoot = AppConfig.get().get("asset.classRoot");
	}

	@Override
	public Object handle(HttpContext context) throws IOException {
		URL url = null;
		String path = context.path();
		if (fileRoot != null) {
			Path file = fileRoot.resolve(path.substring(1)).normalize();
			if (file.startsWith(fileRoot) && Files.exists(file) && Files.isRegularFile(file)) {
				url = file.toUri().toURL();
			}
		}
		if (url == null) {
			url = ClassLoader.getSystemResource(classRoot + path);
		}
		if (url == null) {
			throw new WebException(StatusCode.NOT_FOUND, "Resource not found: " + path);
		}

		context.setResponseType(ContentType.byFileName(path));
		URLConnection connection = url.openConnection();
		long length = connection.getContentLengthLong();
		long lastModified = connection.getLastModified();
		context.setResponseLength(length);
		context.responseHeaders().set(HeaderName.ETAG, '"' + Base64.getEncoder().withoutPadding().encodeToString((lastModified + "-" + length).getBytes()) + '"');
		return connection.getInputStream();
	}
}
