package com.josiahebhomenye.raft.event;


import io.netty.channel.Channel;

public interface InboundEvent {

    Channel sender();
}
