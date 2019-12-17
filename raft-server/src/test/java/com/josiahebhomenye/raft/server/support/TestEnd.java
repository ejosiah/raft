package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Interceptor;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class TestEnd extends Interceptor {

    private final Event terminationEvent;
    private final CountDownLatch testEndLatch;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt.equals(terminationEvent)){
            testEndLatch.countDown();
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        testEndLatch.countDown();
        super.exceptionCaught(ctx, cause);
    }
}