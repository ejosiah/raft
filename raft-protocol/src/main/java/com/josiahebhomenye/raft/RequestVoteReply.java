package com.josiahebhomenye.raft;

import lombok.*;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RequestVoteReply {
    long term;
    boolean voteGranted;
}
