package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.server.core.Follower;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.CommitEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.test.support.LogDomainSupport;
import org.junit.After;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import static com.josiahebhomenye.raft.server.core.NodeState.Id.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LeaderCompletenessGuaranteeTest extends GuaranteeTest implements LogDomainSupport {

    Node leader;

    @After
    public void tearDown() {
        nodes.forEach(node -> node.activePeers().clear());
    }

    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        return new LeaderCompletenessGuarantee(nodes, latch);
    }

    @Test
    public void newly_elected_leaders_should_contain_all_logs_committed_from_previous_terms() {
        Node candidate = nodes.getLast();
        candidate.trigger(new StateTransitionEvent(FOLLOWER, CANDIDATE, candidate));
        candidate.trigger(new StateTransitionEvent(CANDIDATE, FOLLOWER, candidate));

        leader = nodes.getFirst();
        leaderEntries().forEach(entry -> leader.log().add(entry));
        leader.trigger(new CommitEvent(3, null));
        leader.trigger(new StateTransitionEvent(FOLLOWER, CANDIDATE, leader));
        leader.trigger(new StateTransitionEvent(CANDIDATE, LEADER, leader));
        leader.trigger(new StateTransitionEvent(LEADER, FOLLOWER, leader));

        leader = candidate;
        leaderEntries().forEach(entry -> leader.log().add(entry));
        leader.trigger(new CommitEvent(leaderEntries().size(), null));
        leader.trigger(new StateTransitionEvent(FOLLOWER, CANDIDATE, leader));
        leader.trigger(new StateTransitionEvent(CANDIDATE, LEADER, leader));

        assertTrue(guarantee.passed());

    }

    @Test
    public void fail_if_newly_elected_leader_does_not_contain_all_committed_logs_from_previous_terms() {
        Node candidate = nodes.getLast();
        candidate.trigger(new StateTransitionEvent(FOLLOWER, CANDIDATE, candidate));
        candidate.trigger(new StateTransitionEvent(CANDIDATE, FOLLOWER, candidate));

        leader = nodes.getFirst();
        followerWithMissingAndExtraUnCommittedEntries0().forEach(entry -> leader.log().add(entry));
        leader.trigger(new CommitEvent(7, null));
        leader.trigger(new StateTransitionEvent(FOLLOWER, CANDIDATE, leader));
        leader.trigger(new StateTransitionEvent(CANDIDATE, LEADER, leader));
        leader.trigger(new StateTransitionEvent(LEADER, FOLLOWER, leader));

        leader = candidate;
        leaderEntries().forEach(entry -> leader.log().add(entry));
        leader.trigger(new CommitEvent(leaderEntries().size(), null));
        leader.trigger(new StateTransitionEvent(FOLLOWER, CANDIDATE, leader));
        leader.trigger(new StateTransitionEvent(CANDIDATE, LEADER, leader));

        assertFalse(guarantee.passed());
    }
}