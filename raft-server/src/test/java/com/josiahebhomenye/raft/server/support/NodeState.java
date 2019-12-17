package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.log.LogEntry;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;

@Value
@Accessors(fluent = true)
public class NodeState {
    private InetSocketAddress id;
    private InetSocketAddress votedFor;
    private long currentTerm;
    private List<LogEntry> logEntries;
    private boolean leader;

    public static NodeState leader(InetSocketAddress id, long currentTerm, List<LogEntry> entries){
        return new NodeState(id, id, currentTerm, entries, true);
    }

    public static NodeState follower(InetSocketAddress id, InetSocketAddress votedFor, long currentTerm, List<LogEntry> entries){
        return new NodeState(id, votedFor, currentTerm, entries, false);
    }
}
