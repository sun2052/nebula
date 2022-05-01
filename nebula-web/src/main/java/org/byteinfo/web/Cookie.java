package org.byteinfo.web;

public class Cookie {
	private String name;
	private String value;
	private String domain;
	private String path;
	private Integer maxAge;
	private boolean secure;
	private boolean httpOnly;

	public Cookie(String name, String value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public Integer getMaxAge() {
		return maxAge;
	}

	public void setMaxAge(Integer maxAge) {
		this.maxAge = maxAge;
	}

	public boolean isSecure() {
		return secure;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public String toString() {
		StringBuilder buf = new StringBuilder().append(name).append('=').append(value);
		if (domain != null) {
			buf.append("; domain=").append(domain);
		}

		if (path != null) {
			buf.append("; path=").append(path);
		}

		if (maxAge != null) {
			buf.append("; max-age=").append(maxAge);
		}

		if (secure) {
			buf.append("; secure");
		}

		if (httpOnly) {
			buf.append("; httponly");
		}

		return buf.toString();
	}
}
