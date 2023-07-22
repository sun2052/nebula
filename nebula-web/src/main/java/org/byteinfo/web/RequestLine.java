package org.byteinfo.web;

public record RequestLine(String method, String target, String path, String query) {
}
