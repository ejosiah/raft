package com.josiahebhomenye.raft.rpc;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.UUID;


@Getter
@Setter
@Accessors(chain = true)
public abstract class RpcMessage {
    private String id = UUID.randomUUID().toString();
    private String correlationId;
    private InetSocketAddress senderId;
}
