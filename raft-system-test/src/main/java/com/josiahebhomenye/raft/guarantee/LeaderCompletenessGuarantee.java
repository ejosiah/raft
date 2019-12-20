package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.LongStream;

@ChannelHandler.Sharable
public class LeaderCompletenessGuarantee extends Guarantee {

    Node prevLeader;

    public LeaderCompletenessGuarantee(List<Node> nodes, CountDownLatch testEndLatch) {
        super(nodes, testEndLatch);
    }

    @Override
    protected void check(ChannelHandlerContext ctx, Event event) {
        if(event instanceof StateTransitionEvent){
            receivedExpectedEvent = true;
            if(event.as(StateTransitionEvent.class).newState().equals(NodeState.LEADER())){
                if (prevLeader != null) {
                    try(Log prevLeaderLog = prevLeader.log().clone()) {
                        try(Log newLeaderLog = event.as(StateTransitionEvent.class).oldState().node().log().clone() ) {
                            long committed = prevLeader.commitIndex();
                            LongStream.rangeClosed(1, committed).forEach(i -> {
                                if(!prevLeaderLog.get(i).equals(newLeaderLog.get(i))){
                                    fail();
                                }
                            });
                        }
                    }
                }
                prevLeader = event.as(StateTransitionEvent.class).oldState().node();
            }
        }
    }
}
