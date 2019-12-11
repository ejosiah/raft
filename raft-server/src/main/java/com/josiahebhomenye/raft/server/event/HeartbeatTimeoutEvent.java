package com.josiahebhomenye.raft.server.event;

import java.net.InetSocketAddress;

public class HeartbeatTimeoutEvent extends Event {
    public HeartbeatTimeoutEvent(InetSocketAddress source) {
        super(source);
    }
}
