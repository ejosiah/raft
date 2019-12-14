package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.event.StateUpdatedEvent;
import com.josiahebhomenye.raft.event.UpdateStateEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public abstract class StateManager<STATE> extends ChannelDuplexHandler{

    private STATE state;

    public StateManager(){
        state = null;
    }

    public StateManager(STATE state) {
        this.state = state;
    }

    abstract STATE handle(UpdateStateEvent event);

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if(evt instanceof UpdateStateEvent){
            STATE state = handle((UpdateStateEvent)evt);
            ctx.pipeline().fireUserEventTriggered(new StateUpdatedEvent(state, ctx.channel().localAddress()));
        }
        ctx.fireUserEventTriggered(evt);
    }
}
