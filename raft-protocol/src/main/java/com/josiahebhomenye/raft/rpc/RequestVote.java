package com.josiahebhomenye.raft.rpc;

import com.josiahebhomenye.raft.comand.MessageType;
import lombok.*;

import java.net.InetSocketAddress;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class RequestVote extends RpcMessage {
    long term;
    long lastLogIndex;
    long lastLogTerm;
    InetSocketAddress candidateId;
}
