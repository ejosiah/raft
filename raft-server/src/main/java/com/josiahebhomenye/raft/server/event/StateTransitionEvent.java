package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.server.core.NodeState;
import com.sun.istack.internal.localization.NullLocalizable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import static com.josiahebhomenye.raft.server.core.NodeState.*;
import java.net.InetSocketAddress;


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
