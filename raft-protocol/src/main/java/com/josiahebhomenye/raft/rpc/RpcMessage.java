package com.josiahebhomenye.raft.rpc;

import lombok.Data;

@Data
public abstract class RpcMessage {
    private String id;
    private String correlationId;
}
