package org.byteinfo.web;

import org.byteinfo.util.io.IOUtil;
import org.byteinfo.util.misc.Config;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

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
			Path file = pathRoot.resolve(path.substring(1));
			if (Files.exists(file) && Files.isRegularFile(file)) {
				return new Result(file.toUri().toURL(), ContentType.byName(path));
			}
		}

		URL url = IOUtil.classResource(classRoot + path);
		if (url == null) {
			throw new WebException(StatusCode.NOT_FOUND, "Resource not found: " + path);
		}
		return new Result(url, ContentType.byName(path));
	}
}
