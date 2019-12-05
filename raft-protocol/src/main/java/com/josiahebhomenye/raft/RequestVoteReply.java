package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.comand.MessageType;
import lombok.*;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RequestVoteReply {
    long term;
    boolean voteGranted;
}
