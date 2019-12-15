package com.josiahebhomenye.raft.rpc;

import com.josiahebhomenye.raft.comand.MessageType;
import lombok.*;

import java.net.InetSocketAddress;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class RequestVote {
    long term;
    long lastLogIndex;
    long lastLogTerm;
    InetSocketAddress candidateId;
}
