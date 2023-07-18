package org.byteinfo.web;

import org.byteinfo.util.io.IOUtil;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public class AssetHandler implements Handler {
	private final Path pathRoot;
	private final String classRoot;

	public AssetHandler() {
		String path = AppConfig.get().get("asset.pathRoot");
		if (path == null) {
			pathRoot = null;
		} else {
			pathRoot = Path.of(path);
		}
		classRoot = AppConfig.get().get("asset.classRoot");
	}

	@Override
	public Object handle(HttpContext context) throws IOException {
		String path = context.path();
		String type = ContentType.byFileName(path);

		if (pathRoot != null) {
			Path file = pathRoot.resolve(path.substring(1)).normalize();
			if (file.startsWith(pathRoot) && Files.exists(file) && Files.isRegularFile(file)) {
				return Result.of(file.toUri().toURL()).setType(type);
			}
		}

		URL url = IOUtil.classResource(classRoot + path);
		if (url == null) {
			throw new WebException(StatusCode.NOT_FOUND, "Resource not found: " + path);
		}
		return Result.of(url).setType(type);
	}
}
