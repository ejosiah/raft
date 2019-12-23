package com.josiahebhomenye.raft.rpc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class AppendEntriesReply {
    long term;
    long lastApplied;
    boolean success;
}

