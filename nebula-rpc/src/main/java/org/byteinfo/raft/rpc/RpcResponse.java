package org.byteinfo.raft.rpc;

public record RpcResponse(long id, Object result, String error) {
}
