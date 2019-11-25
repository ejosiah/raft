package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.comand.MessageType;
import lombok.*;

import java.net.InetSocketAddress;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RequestVote {
    int term;
    int lastLogIndex;
    int lastLogTerm;
    InetSocketAddress candidateId;
}
