package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.log.LogEntry;
import lombok.Value;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.List;

@Value
@Accessors(fluent = true)
public class NodeStateData {
    private InetSocketAddress id;
    private InetSocketAddress votedFor;
    private long currentTerm;
    private List<LogEntry> logEntries;
    private boolean leader;

    public static NodeStateData leader(InetSocketAddress id, long currentTerm, List<LogEntry> entries){
        return new NodeStateData(id, id, currentTerm, entries, true);
    }

    public static NodeStateData follower(InetSocketAddress id, InetSocketAddress votedFor, long currentTerm, List<LogEntry> entries){
        return new NodeStateData(id, votedFor, currentTerm, entries, false);
    }
}
