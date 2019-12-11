package com.josiahebhomenye.raft.server.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.List;

public class UserEventCapture extends ChannelInboundHandlerAdapter {

    private List<Object> events = new ArrayList<>();

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        events.add(evt);
        ctx.fireUserEventTriggered(evt);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index){
        return (T) (events.size() > index ? events.get(index) : null);
    }

    public void clear(){
        events.clear();
    }
}
