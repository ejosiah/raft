package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class ScheduleTimeoutEvent extends Event {
    private long timeout;
    private TimeUnit unit;

    public ScheduleTimeoutEvent(Channel source, long timeout){
        this(source, timeout, TimeUnit.MILLISECONDS);
    }

    public ScheduleTimeoutEvent(Channel source, long timeout, TimeUnit unit){
        super(source);
        this.timeout = timeout;
        this.unit = unit;
    }
}
