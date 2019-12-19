package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Accessors(fluent = true)
@RequiredArgsConstructor
@ChannelHandler.Sharable
public abstract class Guarantee extends Interceptor {

    protected final List<Node> nodes;
    protected final CountDownLatch testEndLatch;

    @Getter
    private boolean passed = true;

    public void fail(){
        passed = false;
        nodes.forEach(Node::stop);
        testEndLatch.countDown();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof Event){
            check((Event)evt);
        }
        ctx.fireUserEventTriggered(evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        fail();
        ctx.fireExceptionCaught(cause);
    }

    protected abstract void check(Event event);
}
