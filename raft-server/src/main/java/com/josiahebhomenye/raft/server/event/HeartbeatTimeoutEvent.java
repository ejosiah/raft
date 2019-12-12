package com.josiahebhomenye.raft.server.event;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.net.InetSocketAddress;

@Data
@EqualsAndHashCode(callSuper=true)
public class HeartbeatTimeoutEvent extends Event {
    public HeartbeatTimeoutEvent(InetSocketAddress source) {
        super(source);
    }
}
