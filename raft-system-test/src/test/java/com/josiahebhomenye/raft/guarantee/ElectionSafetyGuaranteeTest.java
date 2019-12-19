package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class ElectionSafetyGuaranteeTest extends GuaranteeTest {



    @Test
    public void single_leader_expected_when_a_node_transitions_to_leader(){
        nodes.forEach(node -> {
            node.handle(StateTransitionEvent.initialStateTransition());
        });

        nodes.getFirst()
            .channel()
            .pipeline()
        .fireUserEventTriggered(new StateTransitionEvent(NodeState.FOLLOWER(), NodeState.LEADER(), null));

        assertTrue(guarantee.passed());
    }

    @Test
    public void fail_guarantee_if_more_than_one_leader_elected(){
        nodes.stream().skip(1).forEach(node -> {
            node.handle(StateTransitionEvent.initialStateTransition());
        });
        StateTransitionEvent event = new StateTransitionEvent(NodeState.FOLLOWER(), NodeState.LEADER(), null);
        nodes.getFirst().handle(event);

        nodes.getLast()
            .channel()
            .pipeline()
        .fireUserEventTriggered(event);

        assertFalse(guarantee.passed());
        assertEquals(0, latch.getCount());
    }

    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        return new ElectionSafetyGuarantee(nodes, latch);
    }
}