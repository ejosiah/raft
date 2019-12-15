package com.josiahebhomenye.raft.rpc;

import lombok.*;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AppendEntries)) return false;
        AppendEntries that = (AppendEntries) o;
        return term == that.term &&
                prevLogIndex == that.prevLogIndex &&
                prevLogTerm == that.prevLogTerm &&
                leaderCommit == that.leaderCommit &&
                leaderId.equals(that.leaderId) &&
                IntStream.range(0, entries.size()).allMatch(i -> Arrays.equals(entries.get(i), ((AppendEntries) o).entries.get(i)));
    }

    @Override
    public int hashCode() {
        return Objects.hash(term, prevLogIndex, prevLogTerm, leaderCommit, leaderId, entries);
    }
}


