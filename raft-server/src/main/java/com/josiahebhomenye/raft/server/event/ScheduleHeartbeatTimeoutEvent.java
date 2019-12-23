package com.josiahebhomenye.raft.server.event;

import io.netty.channel.Channel;

import java.net.SocketAddress;

public class ScheduleHeartbeatTimeoutEvent extends ScheduleTimeoutEvent {

    public ScheduleHeartbeatTimeoutEvent(Channel source, long timeout){
        super(source, timeout);
    }
}
