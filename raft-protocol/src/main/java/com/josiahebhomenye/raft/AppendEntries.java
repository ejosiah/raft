package com.josiahebhomenye.raft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
public class AppendEntries {
    private long term;
    private long prevLogIndex;
    private long prevLogTerm;
    private long leaderCommit;
    private InetSocketAddress leaderId;
    private List<byte[]> entries;

    public static AppendEntries heartbeat(long term, long prevLogIndex, long prevLogTerm, long leaderCommit, InetSocketAddress leaderId){
        return new AppendEntries(term, prevLogIndex, prevLogTerm, leaderCommit, leaderId, Collections.emptyList());
    }
}


