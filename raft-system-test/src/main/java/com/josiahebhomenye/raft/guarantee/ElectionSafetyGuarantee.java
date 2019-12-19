package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static com.josiahebhomenye.raft.server.core.NodeState.LEADER;
import static java.util.stream.Collectors.toList;

@ChannelHandler.Sharable
public class ElectionSafetyGuarantee extends Guarantee {

    public ElectionSafetyGuarantee(List<Node> nodes, CountDownLatch testEndLatch) {
        super(nodes, testEndLatch);
    }

    @Override
    protected void check(Event event) {
        if(event instanceof StateTransitionEvent){
            if(event.as(StateTransitionEvent.class).newState().equals(LEADER())){
                int numLeaders =
                        nodes
                            .stream()
                            .map(Node::state)
                            .filter(state -> state.equals(LEADER()))
                            .collect(toList()).size();

                if(numLeaders > 1){
                    fail();
                }
            }
        }
    }
}
