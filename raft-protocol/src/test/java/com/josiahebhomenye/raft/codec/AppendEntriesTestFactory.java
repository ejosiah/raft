package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Set;

import java.net.InetSocketAddress;

public interface AppendEntriesTestFactory {

    default AppendEntries get(){
        int term = 1;
        int prevLogIndex = 1;
        int prevLogTerm = 0;
        Command command = new Set(5);
        int leaderCommit = 3;
        InetSocketAddress leaderId = new InetSocketAddress("localhost", 8080);

        return new AppendEntries(term, prevLogIndex, prevLogTerm, leaderCommit,  leaderId, command.serialize());
    }

    default AppendEntries getHeartBeat(){
        return get().withEntries(new byte[0]);
    }
}
