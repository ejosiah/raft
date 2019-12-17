package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class LeaderStart extends Interceptor {

    private final CountDownLatch leaderStartLatch;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt.equals(StateTransitionEvent.initialStateTransition())){
            leaderStartLatch.await();
        }
        ctx.fireUserEventTriggered(evt);
    }
}
