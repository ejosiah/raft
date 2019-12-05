package com.josiahebhomenye.raft;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@Accessors(fluent = true)
public class AppendEntriesReply {
    long term;
    boolean success;
}

