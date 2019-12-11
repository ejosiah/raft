package com.josiahebhomenye.raft.server.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class ScheduleTimeoutEvent extends Event {
    private long timeout;

    public ScheduleTimeoutEvent(SocketAddress source, long timeout){
        super(source);
        this.timeout = timeout;
    }
}
