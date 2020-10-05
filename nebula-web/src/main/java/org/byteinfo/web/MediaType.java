package org.byteinfo.web;

import org.byteinfo.util.io.IOUtil;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class MediaType {
	// Extension MediaType Mapping
	private static final ConcurrentHashMap<String, MediaType> TYPES = new ConcurrentHashMap<>();

	// Common MediaType
	public static final MediaType ALL = new MediaType("*", "*");
	public static final MediaType TEXT = new MediaType("text", "*");
	public static final MediaType PLAIN = new MediaType("text", "plain");
	public static final MediaType HTML = new MediaType("text", "html");
	public static final MediaType CSS = new MediaType("text", "css");
	public static final MediaType JS = new MediaType("application", "javascript");
	public static final MediaType JSON = new MediaType("application", "json");
	public static final MediaType XML = new MediaType("application", "xml");
	public static final MediaType FORM = new MediaType("application", "x-www-form-urlencoded");
	public static final MediaType OCTETSTREAM = new MediaType("application", "octet-stream");
	public static final MediaType MULTIPART = new MediaType("multipart", "form-data");

	private final String type;
	private final String subtype;
	private final boolean wildcardType;
	private final boolean wildcardSubtype;
	private String name;

	static {
		try {
			Properties properties = new Properties();
			try (Reader in = IOUtil.resourceReader("org/byteinfo/web/mime.properties")) {
				properties.load(in);
			}
			for (Map.Entry<Object, Object> entry : properties.entrySet()) {
				String[] parts = entry.getValue().toString().split("/");
				TYPES.put(entry.getKey().toString(), new MediaType(parts[0], parts[1]));
			}
		} catch (IOException e) {
			throw new WebException("Failed to initialize MediaType.", e);
		}
	}

	/**
	 * Creates a new MediaType.
	 *
	 * @param type    The primary type. Required.
	 * @param subtype The secondary type. Required.
	 */
	public MediaType(String type, String subtype) {
		this.type = type;
		this.subtype = subtype;
		this.wildcardType = "*".equals(type);
		this.wildcardSubtype = "*".equals(subtype);
		this.name = type + "/" + subtype;
	}

	/**
	 * Get the primary media type.
	 *
	 * @return primary media type
	 */
	public String type() {
		return type;
	}

	/**
	 * Get the secondary media type.
	 *
	 * @return secondary media type
	 */
	public String subtype() {
		return subtype;
	}

	/**
	 * Get the qualified type {@link #type()}/{@link #subtype()}.
	 *
	 * @return qualified type
	 */
	public String name() {
		return name;
	}

	/**
	 * Check if the given media type matches the current one.
	 *
	 * @param that a media type to compare to
	 * @return true if matches
	 */
	public boolean matches(MediaType that) {
		if (this == that || this.wildcardType || that.wildcardType) {
			return true; // same or */*
		}
		if (type.equals(that.type)) {
			if (subtype.equals(that.subtype) || this.wildcardSubtype || that.wildcardSubtype) {
				return true;
			}
			if (subtype.startsWith("*")) {
				return that.subtype.endsWith(subtype.substring(1));
			}
		}
		return false;
	}

	/**
	 * @return True, if this type is a well-known text type.
	 */
	public boolean isText() {
		if (this.wildcardType) {
			return false;
		}

		return this == TEXT || TEXT.matches(this);
	}

	/**
	 * Get a MediaType for a file path.
	 *
	 * @param path A candidate file path.
	 * @return A MediaType or empty optional for unknown file extensions.
	 */
	public static Optional<MediaType> byPath(Path path) {
		return byPath(path.toString());
	}

	/**
	 * Get a MediaType for a file path.
	 *
	 * @param path A candidate file path: like <code>myfile.js</code> or <code>/js/myfile.js</code>.
	 * @return A MediaType or empty optional for unknown file extensions.
	 */
	public static Optional<MediaType> byPath(String path) {
		int index = path.lastIndexOf('.');
		if (index != -1) {
			String ext = path.substring(index + 1);
			return byExtension(ext);
		}
		return Optional.empty();
	}

	/**
	 * Get a MediaType for a file extension.
	 *
	 * @param ext A file extension, like <code>js</code> or <code>css</code>.
	 * @return A MediaType or empty optional for unknown file extensions.
	 */
	public static Optional<MediaType> byExtension(String ext) {
		return Optional.ofNullable(TYPES.get(ext));
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (obj instanceof MediaType) {
			MediaType that = (MediaType) obj;
			return type.equals(that.type) && subtype.equals(that.subtype);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}
}
