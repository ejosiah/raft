package com.josiahebhomenye.raft.rpc;

import lombok.*;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RequestVoteReply {
    long term;
    boolean voteGranted;
}
