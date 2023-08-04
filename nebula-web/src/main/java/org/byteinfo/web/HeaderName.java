package org.byteinfo.web;

public interface HeaderName {
	String HOST = "host";
	String CONTENT_TYPE = "content-type";
	String CONTENT_LENGTH = "content-length";
	String CONTENT_DISPOSITION = "content-disposition";
	String TRANSFER_ENCODING = "transfer-encoding";
	String USER_AGENT = "user-agent";
	String CONNECTION = "connection";
	String LOCATION = "location";
	String COOKIE = "cookie";
	String SET_COOKIE = "set-cookie";
	String ETAG = "etag";
	String IF_NONE_MATCH = "if-none-match";
	String FORWARDED_HOST = "x-forwarded-host";
	String FORWARDED_PORT = "x-forwarded-port";
	String FORWARDED_PROTO = "x-forwarded-proto";
	String FORWARDED_FOR = "x-forwarded-for";
	String REQUESTED_WITH = "x-requested-with";
}
