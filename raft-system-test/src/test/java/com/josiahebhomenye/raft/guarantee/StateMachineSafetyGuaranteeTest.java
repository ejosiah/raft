package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.test.support.LogDomainSupport;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class StateMachineSafetyGuaranteeTest extends GuaranteeTest implements LogDomainSupport {

    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        return new StateMachineSafetyGuarantee(nodes, latch);
    }

    @Test
    public void applied_log_entries_in_one_server_should_not_be_different_from_other_servers(){
        nodes.forEach(node -> node.trigger(StateTransitionEvent.initialStateTransition()));

        long term = leaderEntries().getLast().getTerm();
        long leaderCommit = leaderEntries().size();
        long prevLogIndex = 0;
        long prevLogTerm = 0;
        InetSocketAddress leaderId = new InetSocketAddress(8080);
        List<byte[]> entries = leaderEntries().stream().map(LogEntry::serialize).collect(Collectors.toList());


        AppendEntries appendEntries = AppendEntries.heartbeat(term, prevLogIndex, prevLogTerm, leaderCommit, leaderId);
        appendEntries = appendEntries.withEntries(entries);
        AppendEntriesEvent event = new AppendEntriesEvent(appendEntries, clientChannel);

        nodes.forEach(node -> node.trigger(event));

        assertTrue(guarantee.passed());
    }

    @Test
    public void fail_applied_log_entries_one_one_server_does_not_match_applied_log_entries_in_another_server(){
        nodes.forEach(node -> node.trigger(StateTransitionEvent.initialStateTransition(node)));

        long term = leaderEntries().getLast().getTerm();
        long leaderCommit = leaderEntries().size();
        long prevLogIndex = 0;
        long prevLogTerm = 0;
        InetSocketAddress leaderId = new InetSocketAddress(8080);
        List<byte[]> entries = leaderEntries().stream().map(LogEntry::serialize).collect(Collectors.toList());


        AppendEntries appendEntries = AppendEntries.heartbeat(term, prevLogIndex, prevLogTerm, leaderCommit, leaderId);
        appendEntries = appendEntries.withEntries(entries);
        AppendEntriesEvent event = new AppendEntriesEvent(appendEntries, clientChannel);

        nodes.stream().skip(1).forEach(node -> node.trigger(event));

        Node offendingNode = nodes.getFirst();
        List<byte[]> newEntries = followerWithMissingAndExtraUnCommittedEntries1().stream().map(LogEntry::serialize).collect(Collectors.toList());
        leaderCommit = newEntries.size();
        offendingNode.trigger(event.withMsg(appendEntries.withEntries(newEntries).withLeaderCommit(leaderCommit)));

        assertFalse(guarantee.passed());
    }
}