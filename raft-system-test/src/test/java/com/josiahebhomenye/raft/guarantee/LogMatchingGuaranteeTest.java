package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.test.support.LogDomainSupport;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class LogMatchingGuaranteeTest extends GuaranteeTest implements LogDomainSupport {

    private LogMatchingGuarantee guarantee;

    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        guarantee = new LogMatchingGuarantee(nodes, latch);
        return guarantee;
    }

    @Test
    public void entries_should_match_on_all_servers(){
        nodes.forEach(node -> node.trigger(StateTransitionEvent.initialStateTransition()));

        leaderEntries().forEach(entry -> {
            nodes.getFirst().log().add(entry);
        });

        followerMissingEntries0().forEach(entry -> {
            nodes.get(1).log().add(entry);
        });

        followerMissingEntries1().forEach(entry -> {
            nodes.get(2).log().add(entry);
        });

        int lastApplied = followerMissingEntries1().size();
        LogEntry logEntry = followerMissingEntries1().getLast();
        InetSocketAddress id = nodes.get(2).id();

        ApplyEntryEvent event = new ApplyEntryEvent(lastApplied, logEntry, id);

        nodes.get(2).trigger(event);

        assertTrue(guarantee.passed());

    }

    @Test
    public void fail_if_entries_with_the_same_index_dont_match(){
        nodes.forEach(node -> node.trigger(StateTransitionEvent.initialStateTransition()));

        leaderEntries().forEach(entry -> {
            nodes.getFirst().log().add(entry);
        });

        followerMissingEntries0().forEach(entry -> {
            nodes.get(1).log().add(entry);
        });

        followerMissingEntries1().forEach(entry -> {
            nodes.get(2).log().add(entry);
        });

        int lastApplied = followerMissingEntries1().size();
        LogEntry logEntry = followerMissingEntries1().getLast();
        InetSocketAddress id = nodes.get(2).id();

        LogEntry entry = new LogEntry(1, new Set(4).serialize());
        long index = lastApplied - 1;
        nodes.getFirst().log().add(entry, index);

        ApplyEntryEvent event = new ApplyEntryEvent(lastApplied, logEntry, id);

        nodes.get(2).trigger(event);

        assertFalse(guarantee.passed());
    }
}