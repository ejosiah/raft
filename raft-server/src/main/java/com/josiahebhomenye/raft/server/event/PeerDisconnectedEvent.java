package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Peer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class PeerDisconnectedEvent extends Event {
    private Peer peer;

    public PeerDisconnectedEvent(Peer peer) {
        super(peer.channel());
        this.peer = peer;
    }
}
