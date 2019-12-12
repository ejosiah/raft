package com.josiahebhomenye.raft.server.core;

import io.netty.channel.ChannelDuplexHandler;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
public abstract class Interceptor extends ChannelDuplexHandler {

    @Setter
    private Node node;
}
