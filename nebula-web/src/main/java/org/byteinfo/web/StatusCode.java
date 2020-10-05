package org.byteinfo.web;

public interface StatusCode {
	int OK = 200;

	int MOVED_PERMANENTLY = 301;
	int SEE_OTHER = 303;
	int NOT_MODIFIED = 304;

	int BAD_REQUEST = 400;
	int UNAUTHENTICATED = 401;
	int UNAUTHORIZED = 403;
	int NOT_FOUND = 404;

	int INTERNAL_SERVER_ERROR = 500;
}
