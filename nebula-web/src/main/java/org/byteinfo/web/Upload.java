package org.byteinfo.web;

public record Upload(String name, String originalName, String contentType, byte[] bytes) {
}
