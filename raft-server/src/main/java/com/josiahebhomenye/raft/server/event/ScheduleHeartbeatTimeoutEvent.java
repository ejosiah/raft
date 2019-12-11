package com.josiahebhomenye.raft.server.event;

import java.net.SocketAddress;

public class ScheduleHeartbeatTimeoutEvent extends ScheduleTimeoutEvent {

    public ScheduleHeartbeatTimeoutEvent(SocketAddress source, long timeout){
        super(source, timeout);
    }
}
