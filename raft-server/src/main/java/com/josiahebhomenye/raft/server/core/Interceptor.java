package com.josiahebhomenye.raft.server.core;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;


public abstract class Interceptor extends ChannelDuplexHandler {


    public Node node(ChannelHandlerContext ctx){
        return (Node)ctx.pipeline().context(Node.class).handler();
    }

}
