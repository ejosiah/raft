package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.comand.MessageType;
import lombok.*;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RequestVoteReply {
    int term;
    boolean voteGranted;
}
