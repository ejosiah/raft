package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.server.core.NodeState;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import java.net.InetSocketAddress;
import static com.josiahebhomenye.raft.server.core.NodeState.*;


@Value
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class StateTransitionEvent extends Event {
    private NodeState oldState;
    private NodeState newState;

    public StateTransitionEvent(NodeState oldState, NodeState newState, InetSocketAddress source){
        super(source);
        this.oldState = oldState;
        this.newState = newState;
    }

    public static StateTransitionEvent initialStateTransition(){
        return new StateTransitionEvent(NULL_STATE(), FOLLOWER(), null);
    }
}
