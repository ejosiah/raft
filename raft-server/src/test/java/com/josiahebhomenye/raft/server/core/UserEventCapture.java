package com.josiahebhomenye.raft.server.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class UserEventCapture extends ChannelInboundHandlerAdapter {

    private Object event;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        this.event = evt;
        ctx.fireUserEventTriggered(evt);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(){
        return (T)event;
    }
}
