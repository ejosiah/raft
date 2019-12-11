package com.josiahebhomenye.raft.server.event;

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

    public ElectionTimeoutEvent(Instant lastheartbeat, InetSocketAddress source){
        super(source);
        this.lastheartbeat = lastheartbeat;
    }
}
