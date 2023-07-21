package org.byteinfo.web;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

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
		String type = ContentType.byFileName(path);

		if (fileRoot != null) {
			Path file = fileRoot.resolve(path.substring(1)).normalize();
			if (file.startsWith(fileRoot) && Files.exists(file) && Files.isRegularFile(file)) {
				return Result.of(file.toUri().toURL()).setType(type);
			}
		}

		URL url = ClassLoader.getSystemResource(classRoot + path);
		if (url == null) {
			throw new WebException(StatusCode.NOT_FOUND, "Resource not found: " + path);
		}
		return Result.of(url).setType(type);
	}
}
