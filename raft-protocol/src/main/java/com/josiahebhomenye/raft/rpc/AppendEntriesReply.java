package com.josiahebhomenye.raft.rpc;

import lombok.*;
import lombok.experimental.Accessors;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AppendEntriesReply extends RpcMessage {
    long term;
    long lastApplied;
    boolean success;
}

