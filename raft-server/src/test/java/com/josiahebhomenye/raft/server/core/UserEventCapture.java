package com.josiahebhomenye.raft.server.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UserEventCapture extends ChannelDuplexHandler {

    private List<Object> events = new ArrayList<>();
    private List<Class<?>> ignoreList = new ArrayList<>();

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(!ignore(evt)) {
            events.add(evt);
        }
        ctx.fireUserEventTriggered(evt);
    }

    private boolean ignore(Object event){
        return ignoreList.contains(event.getClass());
    }

    public void ignore(Class<?>... classes){
        ignoreList.addAll(Arrays.asList(classes));
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int index){
        return (T) (events.size() > index ? events.get(index) : null);
    }

    public void clear(){
        events.clear();
    }

    public int captured(){
        return events.size();
    }
}
