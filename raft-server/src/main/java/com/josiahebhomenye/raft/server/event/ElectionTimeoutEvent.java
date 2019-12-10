package com.josiahebhomenye.raft.server.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.time.Instant;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class ElectionTimeoutEvent extends Event {
    public Instant lastheartbeat;

    public ElectionTimeoutEvent(Instant lastheartbeat, InetSocketAddress source){
        super(source);
        this.lastheartbeat = lastheartbeat;
    }
}
