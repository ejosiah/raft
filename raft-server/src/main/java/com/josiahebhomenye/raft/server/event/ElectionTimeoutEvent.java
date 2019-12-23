package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.time.Instant;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class ElectionTimeoutEvent extends Event {
    public Instant lastheartbeat;

    public ElectionTimeoutEvent(Instant lastheartbeat, Channel source){
        super(source);
        this.lastheartbeat = lastheartbeat;
    }
}
