package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteReplyEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * at most one leader can be elected in a given term.
 */
@Slf4j
@ChannelHandler.Sharable
public class ElectionSafetyGuarantee extends Guarantee {

    private final int majority;

    public ElectionSafetyGuarantee(List<Node> nodes, CountDownLatch testEndLatch, int majority) {
        super(nodes, testEndLatch);
        this.majority = majority;
    }


    @Override
    protected void check(Node source, Event event) {
        if(event instanceof StateTransitionEvent){
            receivedExpectedEvent = true;
            StateTransitionEvent evt = event.as(StateTransitionEvent.class);
            if(evt.newState().isLeader()){
                if(leaderCount(source.currentTerm()) > 1){
                    log.info("multiple leaders encounters");
                    nodes.forEach(n -> log.info("{}", n));
                    fail();
                }
            }
        }
    }

    private long leaderCount(long term){
        return nodes.stream().filter(n -> n.currentTerm() == term && n.isLeader()).count();
    }
}
