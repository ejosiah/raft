package com.josiahebhomenye.raft.client;

import io.netty.buffer.ByteBuf;
import lombok.Value;

import java.util.concurrent.CompletableFuture;

@Value
public class PromiseRequest {
    CompletableFuture<?> promise;
    String id;
    ByteBuf request;
}
