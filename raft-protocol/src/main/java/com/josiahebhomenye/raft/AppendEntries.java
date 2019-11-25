package com.josiahebhomenye.raft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.net.InetSocketAddress;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class AppendEntries {
    private int term;
    private int prevLogIndex;
    private int prevLogTerm;
    private int leaderCommit;
    private InetSocketAddress leaderId;
    private byte[] entries;

    public static AppendEntries heartbeat(int term, int prevLogIndex, int prevLogTerm, int leaderCommit, InetSocketAddress leaderId){
        return new AppendEntries(term, prevLogIndex, prevLogTerm, leaderCommit, leaderId, new byte[0]);
    }
}


