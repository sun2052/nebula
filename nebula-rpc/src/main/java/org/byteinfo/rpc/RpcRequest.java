package org.byteinfo.rpc;

public record RpcRequest(long id, String service, String method, Class<?>[] params, Object[] args) {
}
