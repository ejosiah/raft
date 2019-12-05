package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.server.core.NodeState;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;


@Value
@Accessors(fluent = true)
public class StateTransitionEvent extends Event {
    private NodeState oldState;
    private NodeState newState;

    public StateTransitionEvent(NodeState oldState, NodeState newState, InetSocketAddress source){
        super(source);
        this.oldState = oldState;
        this.newState = newState;
    }
}
