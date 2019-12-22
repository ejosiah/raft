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

    Map<CandidateId, Integer> votes = new HashMap<>();
    final int majority;

    public ElectionSafetyGuarantee(List<Node> nodes, CountDownLatch testEndLatch, int majority) {
        super(nodes, testEndLatch);
        this.majority = majority;
    }


    @Override
    protected void check(Node source, Event event) {
        if(event instanceof RequestVoteReplyEvent && event.as(RequestVoteReplyEvent.class).voteGranted()){
            receivedExpectedEvent = true;
            CandidateId id = new CandidateId(source.currentTerm(), source);
            votes.put(id, votes.getOrDefault(id, 1)+1);
        }else if(event instanceof StateTransitionEvent){
            StateTransitionEvent evt = event.as(StateTransitionEvent.class);
            if(evt.newState().isLeader()){
                if(leaderCount() > 1){
                    fail();
                }
            }
        }else if(event instanceof AppendEntriesEvent){
            List<CandidateId> candidates =
                    votes.keySet()
                        .stream()
                        .filter(id -> id.term == ((AppendEntriesEvent) event).msg().getTerm())
                        .collect(Collectors.toList());

            candidates.forEach(votes::remove);
        }
    }

    protected long leaderCount(){
        return votes.keySet().stream().filter(id -> !id.node.stopped()).filter(this::majorityVotesGranted).count();
    }

    protected boolean majorityVotesGranted(CandidateId id){
        float totalVotes = activeNodes()+1;
        float vote = votes.get(id);
        return  vote >= majority || (totalVotes != 1 && vote/totalVotes >= 0.5);
    }

    private float activeNodes(){
        return nodes.stream().filter(node -> !node.stopped()).count();
    }

    public int totalVotes(){ // FIXME votes should be per term
        return votes.values().stream().reduce(0, Integer::sum);
    }

    @Value
    private static class CandidateId {
        private final long term;
        private final Node node;
    }
}
