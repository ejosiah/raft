package com.josiahebhomenye.raft.rpc;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.comand.Command;
import lombok.*;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
public class Redirect extends RpcMessage {
    private InetSocketAddress leaderId;
    private Request request;
}
