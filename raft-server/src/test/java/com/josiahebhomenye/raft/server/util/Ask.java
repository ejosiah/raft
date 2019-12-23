package com.josiahebhomenye.raft.server.util;

import com.josiahebhomenye.raft.rpc.RpcMessage;
import io.netty.channel.Channel;

import java.util.concurrent.CompletableFuture;

public class Ask {
    private Channel channel;

   public <T extends RpcMessage> CompletableFuture<T> get(RpcMessage msg){
       return null;
   }
}
