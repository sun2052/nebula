package org.byteinfo.rpc;

public record RpcResponse(long id, Object result, String error) {
}
