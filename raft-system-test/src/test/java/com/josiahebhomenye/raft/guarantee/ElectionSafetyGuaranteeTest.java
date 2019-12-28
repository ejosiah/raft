package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteReplyEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

import static com.josiahebhomenye.raft.server.core.NodeState.Id.*;
import static org.junit.Assert.*;

public class ElectionSafetyGuaranteeTest extends GuaranteeTest {

    private ElectionSafetyGuarantee guarantee;

    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        guarantee = new ElectionSafetyGuarantee(nodes, latch);
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
    public void pass_if_previous_leader_offline_and_new_leader_elected_for_higher_term() throws Exception{
        grantVote(nodes.getLast());
        electAsLeader(nodes.getFirst());
        revertToFollower(nodes.getLast());

        nodes.getFirst().stop().get();
        electAsLeader(nodes.getLast());

        assertTrue(guarantee.passed());
    }


    @Test
    public void pass_after_leader_of_previous_term_comes_back_online_new_leader_elected_for_higher_term() throws Exception{
        grantVote(nodes.getLast());
        electAsLeader(nodes.getFirst());
        revertToFollower(nodes.getLast());

        nodes.getFirst().stop().get(); // TODO use reflection to set value instead
        electAsLeader(nodes.getLast());
        nodes.getFirst().restart();

        assertTrue(guarantee.passed());
    }

    @Test
    public void single_leader_should_be_elected_when_available_nodes_less_than_majority(){
       nodes.stream().skip(2).forEach(node -> uncheck( () -> node.stop().get()));
       electAsLeader(nodes.getFirst(), 1);
       assertTrue(guarantee.passed());
    }

    @Test
    public void fail_if_leaders_elected_after_split_votes_between_every_node(){
        nodes.forEach(n -> electAsLeader(n, 0));

        assertFalse(guarantee.passed());
    }

    @Test
    public void fail_leaders_from_split_votes(){
        electAsLeader(nodes.getFirst(), 1);
        electAsLeader(nodes.getLast(), 1);

        assertFalse(guarantee.passed());
    }

    private void grantVote(Node candidate){
        if(!candidate.isCandidate()){
            candidate.trigger(new StateTransitionEvent(candidate.state().id(), CANDIDATE, candidate));
        }
        RequestVoteReply vote = new RequestVoteReply(1, true);
        candidate.trigger(new RequestVoteReplyEvent(vote, clientChannel));
    }

    private void electAsLeader(Node candidate){
        electAsLeader(candidate, config.majority - 1);
    }

    private void electAsLeader(Node candidate, int count){
        if(!candidate.isCandidate()){
            candidate.trigger(new StateTransitionEvent(candidate.state().id(), CANDIDATE, candidate));
        }
        IntStream.range(0, count).forEach(i -> {
            RequestVoteReply vote = new RequestVoteReply(1, true);
            candidate.trigger(new RequestVoteReplyEvent(vote, clientChannel));
        });
        candidate.trigger(new StateTransitionEvent(FOLLOWER, LEADER, candidate));
    }

    private void declineVote(Node candidate){
        declineVote(candidate, 0);
    }

    private void declineVote(Node candidate, int count){
        if(!candidate.isCandidate()){
            candidate.trigger(new StateTransitionEvent(candidate.state().id(), CANDIDATE, candidate));
        }
        IntStream.range(0, count).forEach(i -> {
            RequestVoteReply vote = new RequestVoteReply(1, false);
            candidate.trigger(new RequestVoteReplyEvent(vote, clientChannel));
        });
        if(count > 1) {
            candidate.trigger(new StateTransitionEvent(CANDIDATE, FOLLOWER, candidate));
        }
    }

    private void startLeadDuties(Node leader){
        AppendEntries heartbeat = AppendEntries.heartbeat(1, 0, 0, 0, nodes.getFirst().id());
        AppendEntriesEvent event = new AppendEntriesEvent(heartbeat, clientChannel);
        leader.trigger(event);
    }

    private void revertToFollower(Node candidate) {
        if(!candidate.isFollower()){
            candidate.trigger(new StateTransitionEvent(candidate.state().id(), FOLLOWER, candidate));
        }
    }

}