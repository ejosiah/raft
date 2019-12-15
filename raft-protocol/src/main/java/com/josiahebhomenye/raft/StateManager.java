package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.event.StateUpdatedEvent;
import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public abstract class StateManager<STATE> extends ChannelDuplexHandler{

    protected STATE state;

    public StateManager(){
        state = null;
    }

    public StateManager(STATE state) {
        this.state = state;
    }

    abstract STATE handle(ApplyEntryEvent event);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if(evt instanceof ApplyEntryEvent){
            STATE state = handle((ApplyEntryEvent)evt);
            ctx.pipeline().fireUserEventTriggered(new StateUpdatedEvent(state, ctx.channel().localAddress()));
        }
        ctx.fireUserEventTriggered(evt);
    }
}
