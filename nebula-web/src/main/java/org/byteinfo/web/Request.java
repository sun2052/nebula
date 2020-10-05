package org.byteinfo.web;

import java.io.InputStream;

public record Request(String method, String path, String query, Headers headers, long length, InputStream body) {}
