package com.josiahebhomenye.raft.rpc;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.comand.Command;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.net.InetSocketAddress;

@Data
@With
@AllArgsConstructor
@NoArgsConstructor
public class Redirect {
    private InetSocketAddress leaderId;
    private Request request;
}
