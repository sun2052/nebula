package org.byteinfo.raft.socket;

public record Message(byte[] data, Address origin) {
}
