package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Follower;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import io.netty.channel.Channel;
import lombok.EqualsAndHashCode;
import lombok.Value;
import lombok.experimental.Accessors;
import com.josiahebhomenye.raft.server.core.NodeState.Id;
import static com.josiahebhomenye.raft.server.core.NodeState.Id.*;
import static com.josiahebhomenye.raft.io.NullChannel.*;


@Value
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class StateTransitionEvent extends Event {
    private NodeState oldState;
    private NodeState newState;


    public StateTransitionEvent(Id oldStateId, Id newStateId, Node node){
        this(node.get(oldStateId), node.get(newStateId), node.channel() == null ? NULL_CHANNEL : node.channel());
    }
    public StateTransitionEvent(Id oldStateId, Id newStateId, Node node, Channel source){
        this(node.get(oldStateId), node.get(newStateId), source);
    }

    public StateTransitionEvent(NodeState oldState, NodeState newState, Channel source){
        super(source);
        this.oldState = oldState;
        this.newState = newState;
    }

    public static StateTransitionEvent initialStateTransition(Node node){
        return new StateTransitionEvent(NOTHING, FOLLOWER, node);
    }
    public static StateTransitionEvent initialStateTransition(){
        return new StateTransitionEvent(new NodeState.NullState(), Follower.getInstance(), NULL_CHANNEL);
    }
}
