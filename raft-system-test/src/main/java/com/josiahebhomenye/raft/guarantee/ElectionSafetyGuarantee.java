package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Leader;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteReplyEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static com.josiahebhomenye.raft.server.core.NodeState.LEADER;
import static java.util.stream.Collectors.toList;

@Slf4j
@ChannelHandler.Sharable
public class ElectionSafetyGuarantee extends Guarantee {

    Map<InetSocketAddress, Integer> votes = new HashMap<>();
    final int majority;

    public ElectionSafetyGuarantee(List<Node> nodes, CountDownLatch testEndLatch, int majority) {
        super(nodes, testEndLatch);
        this.majority = majority;
    }


    @Override
    protected void check(ChannelHandlerContext ctx, Event event) {
        if(event instanceof RequestVoteReplyEvent && event.as(RequestVoteReplyEvent.class).voteGranted()){
            receivedExpectedEvent = true;
            InetSocketAddress id = source(ctx).id();
            votes.put(id, votes.getOrDefault(id, 0)+1);
        }else if(event instanceof StateTransitionEvent){
            StateTransitionEvent evt = event.as(StateTransitionEvent.class);
            if(evt.newState().isLeader()){
                if(leaderCount() > 1){
                    fail();
                }
            }
        }else if(event instanceof AppendEntriesEvent){
            votes.clear();
        }
    }

    protected long leaderCount(){
        return votes.keySet().stream().filter(this::majorityVotesGranted).count();
    }

    protected boolean majorityVotesGranted(InetSocketAddress id){
        float vote = votes.get(id);
        return  vote >= majority || vote/activeNodes() >= 0.5;
    }

    private float activeNodes(){
        return nodes.stream().filter(node -> !node.stopped()).count();
    }

    public int totalVotes(){
        return votes.values().stream().reduce(0, Integer::sum);
    }
}
