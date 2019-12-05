package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.Peer;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PeerConnectedEvent extends Event {

    public PeerConnectedEvent(Peer peer){
        super(peer.getId());
    }
}
