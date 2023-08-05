package org.byteinfo.web;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
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
		String path = context.path();
		URL url = null;

		// check file path
		if (fileRoot != null) {
			url = getFileAsset(Path.of(fileRoot + path), fileRoot);
		}

		// check classpath
		if (url == null) {
			url = ClassLoader.getSystemResource(classRoot + path);
			if (url != null && "file".equals(url.getProtocol())) { // if in IDE
				url = getFileAsset(new File(url.getPath()).toPath(), new File(ClassLoader.getSystemResource(classRoot).getPath()).toPath());
			}
		}

		// ignore directory
		if (url == null || url.getPath().endsWith("/")) {
			throw new WebException(StatusCode.NOT_FOUND, "Asset not found: " + path);
		}

		// set asset info
		context.setResponseType(ContentType.byFileName(path));
		URLConnection connection = url.openConnection();
		long length = connection.getContentLengthLong();
		long lastModified = connection.getLastModified();
		context.setResponseLength(length);
		context.responseHeaders().set(HeaderName.ETAG, '"' + Base64.getEncoder().withoutPadding().encodeToString((lastModified + "-" + length).getBytes()) + '"');
		return connection.getInputStream();
	}

	private URL getFileAsset(Path asset, Path root) throws MalformedURLException {
		asset = asset.normalize();
		if (asset.startsWith(root) && Files.exists(asset) && Files.isRegularFile(asset)) {
			return asset.toUri().toURL();
		}
		return null;
	}
}
