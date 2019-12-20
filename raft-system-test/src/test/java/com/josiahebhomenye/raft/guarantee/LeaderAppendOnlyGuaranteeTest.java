package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Divide;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.ReceivedRequestEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.raft.server.util.CheckedExceptionWrapper;
import com.josiahebhomenye.raft.server.util.Dynamic;
import com.josiahebhomenye.test.support.LogDomainSupport;
import org.junit.Test;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import static org.junit.Assert.*;

public class LeaderAppendOnlyGuaranteeTest extends GuaranteeTest implements LogDomainSupport, CheckedExceptionWrapper {

    private LeaderAppendOnlyGuarantee leaderAppendOnlyGuarantee;

    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        leaderAppendOnlyGuarantee = new LeaderAppendOnlyGuarantee(nodes, latch);
        return leaderAppendOnlyGuarantee;
    }

    @Test
    public void leader_should_never_override_or_delete_entries(){
        Node leader = nodes.getFirst();
        leader.trigger(new StateTransitionEvent(NodeState.FOLLOWER().set(leader), NodeState.LEADER(), null));

        leaderEntries()
            .stream()
            .map(entry -> new ReceivedRequestEvent(new Request(entry.getCommand()), clientChannel))
            .forEach(leader::trigger);

        assertTrue(guarantee.passed());
    }

    @Test
    public void fail_if_leader_overrides_entries(){
        Node leader = nodes.getFirst();
        leader.trigger(new StateTransitionEvent(NodeState.FOLLOWER().set(leader), NodeState.LEADER(), null));

        leaderEntries()
                .stream()
                .map(entry -> new ReceivedRequestEvent(new Request(entry.getCommand()), clientChannel))
                .forEach(leader::trigger);

        leader.log().add(new LogEntry(0, new Divide(55).serialize()), 1);

        leader.trigger(new ReceivedRequestEvent(new Request(logEntries().getFirst().getCommand()), clientChannel));

        assertFalse(guarantee.passed());
    }

    @Test
    public void fail_if_leader_deletes_entries() throws Exception{
        Node leader = nodes.getFirst();
        leader.trigger(new StateTransitionEvent(NodeState.FOLLOWER().set(leader), NodeState.LEADER(), null));

        leaderEntries()
                .stream()
                .map(entry -> new ReceivedRequestEvent(new Request(entry.getCommand()), clientChannel))
                .forEach(leader::trigger);

        RandomAccessFile data = Dynamic.getField("data", leader.log());
        long remove2Entries = uncheck(data::length) -  2 * (Command.SIZE + Long.SIZE/8);
        data.setLength(remove2Entries);

        leader.trigger(new ReceivedRequestEvent(new Request(logEntries().getFirst().getCommand()), clientChannel));

        assertFalse(guarantee.passed());
    }

    @Test
    public void do_not_run_when_leader_goes_offline() throws Exception{
        Node leader = nodes.getFirst();
        leader.trigger(new StateTransitionEvent(NodeState.FOLLOWER().set(leader), NodeState.LEADER(), null));

        leaderEntries()
                .stream()
                .map(entry -> new ReceivedRequestEvent(new Request(entry.getCommand()), clientChannel))
                .forEach(leader::trigger);

        leader.log().clear();
        leader.stop().get();
        System.out.println(leader);

        nodes.getLast().trigger(new ReceivedRequestEvent(new Request(logEntries().getFirst().getCommand()), clientChannel));

        assertTrue(guarantee.passed());
    }

    @Test
    public void do_not_process_request_if_its_not_from_current_leader(){
        Node leader = nodes.getFirst();
        leader.trigger(new StateTransitionEvent(NodeState.FOLLOWER().set(leader), NodeState.LEADER(), null));

        leaderEntries()
                .stream()
                .map(entry -> new ReceivedRequestEvent(new Request(entry.getCommand()), clientChannel))
                .forEach(leader::trigger);

        Node follower = nodes.getLast();
        follower.trigger(StateTransitionEvent.initialStateTransition());

        leaderEntries()
            .stream()
            .map(entry -> new ReceivedRequestEvent(new Request(entry.getCommand()), clientChannel))
            .forEach(follower::trigger);

        assertTrue(guarantee.passed());
    }
}
