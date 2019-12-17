package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.server.support.NodeState;
import com.josiahebhomenye.raft.server.support.RaftScenarios;
import com.josiahebhomenye.test.support.LogDomainSupport;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class MissingEntriesScenario extends RaftScenarios implements LogDomainSupport {
    @Override
    protected List<NodeState> nodeStates() {
        return new ArrayList<NodeState>(){
            {
                add(NodeState.leader(new InetSocketAddress(9000), 8L, leaderEntries()));
                add(NodeState.follower(new InetSocketAddress(9001), new InetSocketAddress(9000), 6, followerMissingEntries0()));
                add(NodeState.follower(new InetSocketAddress(9002), new InetSocketAddress(9000), 4, followerMissingEntries1()));
            }
        };
    }
}
