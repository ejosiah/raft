package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Subtract;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.support.NodeStateData;
import com.josiahebhomenye.raft.server.support.RaftScenarios;
import com.josiahebhomenye.test.support.LogDomainSupport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class MissingEntriesScenario extends RaftScenarios implements LogDomainSupport {
    @Override
    protected List<NodeStateData> nodeStates() {
        return new ArrayList<NodeStateData>(){
            {
                add(NodeStateData.leader(new InetSocketAddress(9000), 8L, leaderEntries()));
                add(NodeStateData.follower(new InetSocketAddress(9001), new InetSocketAddress(9000), 6, followerMissingEntries0()));
                add(NodeStateData.follower(new InetSocketAddress(9002), new InetSocketAddress(9000), 4, followerMissingEntries1()));
            }
        };
    }

    @Override
    protected List<LogEntry> newEntries() {
        return new ArrayList<LogEntry>(){
            {
                add(new LogEntry(8, new Subtract(2).serialize()));
            }
        };
    }
}
