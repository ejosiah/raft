package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteReplyEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static com.josiahebhomenye.raft.server.core.NodeState.*;
import static org.junit.Assert.*;

public class ElectionSafetyGuaranteeTest extends GuaranteeTest {

    private ElectionSafetyGuarantee guarantee;

    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        guarantee = new ElectionSafetyGuarantee(nodes, latch, 3);
        return guarantee;
    }

    @Test
    public void single_leader_expected_when_a_node_transitions_to_leader(){
        electAsLeader(nodes.getFirst());
        declineVote(nodes.getLast(), 2);

        assertTrue(guarantee.passed());
    }

    @Test
    public void fail_guarantee_if_more_than_one_leader_elected(){
        electAsLeader(nodes.getFirst());
        electAsLeader(nodes.getLast());

        assertFalse(guarantee.passed());
        assertEquals(0, latch.getCount());
    }

    @Test
    public void rest_votes_when_leader_elected() throws Exception{

        electAsLeader(nodes.getFirst());
        grantVote(nodes.getLast());
        declineVote(nodes.getLast(), 1);

        assertEquals(4, guarantee.totalVotes());

        assertTrue(guarantee.passed());

        startLeadDuties(nodes.getFirst());

        assertEquals(0, guarantee.totalVotes());
    }

    @Test
    public void single_leader_should_be_elected_when_available_nodes_less_than_majority(){
       nodes.stream().skip(2).forEach(node -> uncheck( () -> node.stop().get()));
       electAsLeader(nodes.getFirst(), 1);
       assertTrue(guarantee.passed());
    }

    @Test
    public void fail_when_more_than_one_leader_elected_when_available_nodes_less_than_majority(){
        nodes.stream().skip(2).forEach(node -> uncheck( () -> node.stop().get()));
        electAsLeader(nodes.getFirst(), 1);
        electAsLeader(nodes.get(1), 1);
        assertFalse(guarantee.passed());
    }

    private void grantVote(Node candidate){
        RequestVoteReply vote = new RequestVoteReply(1, true);
        candidate.trigger(new RequestVoteReplyEvent(vote, clientChannel));
    }

    private void electAsLeader(Node candidate){
        electAsLeader(candidate, config.majority);
    }

    private void electAsLeader(Node candidate, int count){
        IntStream.range(0, count).forEach(i -> {
            RequestVoteReply vote = new RequestVoteReply(1, true);
            candidate.trigger(new RequestVoteReplyEvent(vote, clientChannel));
            candidate.trigger(new StateTransitionEvent(FOLLOWER().set(candidate), LEADER(), null));
        });
    }

    private void declineVote(Node candidate){
        declineVote(candidate, 0);
    }

    private void declineVote(Node candidate, int count){
        IntStream.range(0, count).forEach(i -> {
            RequestVoteReply vote = new RequestVoteReply(1, false);
            candidate.trigger(new RequestVoteReplyEvent(vote, clientChannel));
            if(count > 1) {
                candidate.trigger(new StateTransitionEvent(CANDIDATE().set(candidate), FOLLOWER(), null));
            }
        });
    }

    private void startLeadDuties(Node leader){
        AppendEntries heartbeat = AppendEntries.heartbeat(1, 0, 0, 0, nodes.getFirst().id());
        AppendEntriesEvent event = new AppendEntriesEvent(heartbeat, null);
        leader.trigger(event);
    }

}