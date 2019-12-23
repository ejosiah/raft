package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.raft.server.event.StopEvent;
import com.josiahebhomenye.raft.server.util.Dynamic;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Accessors(fluent = true)
@RequiredArgsConstructor
public abstract class Guarantee extends Interceptor {

    protected final List<Node> nodes;
    protected final CountDownLatch testEndLatch;
    private boolean failed;
    protected boolean receivedExpectedEvent;

    public void fail(){
        failed = true;
        nodes.forEach(Node::stop);
        testEndLatch.countDown();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!testComplete()) {
            if(evt instanceof Event){
                synchronized (this) {
                    Dynamic.invoke(this, "check", source(ctx), evt).ifPresent(it -> receivedExpectedEvent = true);
                }
            }
        }
        ctx.fireUserEventTriggered(evt);
    }

    public boolean passed(){
        if(!receivedExpectedEvent){
            throw new IllegalStateException(String.format("%s did not execute", getClass().getSimpleName()));
        }
        return !failed;
    }

    public Guarantee setup(){
        return this;
    }

    public Guarantee tearDown(){
        return this;
    }

    public boolean testComplete(){
        return testEndLatch.getCount() <= 0;
    }

    protected Node source(ChannelHandlerContext ctx){
        return (Node)ctx.pipeline().context(Node.class).handler();
    }

}
