package com.ljz.agent.dubbo.model;

import io.netty.buffer.ByteBuf;

public class RpcResponse {

    private long requestId;
    private ByteBuf value;

    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public ByteBuf getValue() {
        return value;
    }

    public void setValue(ByteBuf value) {
        this.value = value;
    }
}
