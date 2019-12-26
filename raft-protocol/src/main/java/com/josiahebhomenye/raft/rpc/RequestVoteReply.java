package com.josiahebhomenye.raft.rpc;

import lombok.*;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestVoteReply extends RpcMessage {
    long term;
    boolean voteGranted;
}
