package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.support.NodeState;
import com.josiahebhomenye.raft.server.support.RaftScenarios;
import com.josiahebhomenye.test.support.LogDomainSupport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class ExtraEntriesScenario extends RaftScenarios implements LogDomainSupport {
    @Override
    protected List<NodeState> nodeStates() {
        return new ArrayList<NodeState>(){
            {
                add(NodeState.leader(new InetSocketAddress(9000), 8L, leaderEntries()));
                add(NodeState.follower(new InetSocketAddress(9001), new InetSocketAddress(9000), 6, followerWithExtraUnCommittedEntries0()));
                add(NodeState.follower(new InetSocketAddress(9002), new InetSocketAddress(9000), 7, followerWithExtraUnCommittedEntries1()));
            }
        };
    }

    @Override
    protected List<LogEntry> newEntries() {
        return new ArrayList<LogEntry>(){
            {
                add(new LogEntry(8, new Set(24).serialize()));
                add(new LogEntry(8, new Set(96).serialize()));
            }
        };
    }
}
