package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Peer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=true)
public class HeartbeatTimeoutEvent extends Event {

    private Peer peer;

    public HeartbeatTimeoutEvent(Peer peer) {
        super(peer.channel());
        this.peer = peer;
    }
}
