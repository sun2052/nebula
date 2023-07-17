package org.byteinfo.web;

public record RequestLine(String method, String path, String query) {
}
