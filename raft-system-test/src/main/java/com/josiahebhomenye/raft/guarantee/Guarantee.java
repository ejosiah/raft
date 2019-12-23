package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.raft.server.event.StopEvent;
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

    @Setter
    protected Node leader;

    public void fail(){
        failed = true;
        nodes.forEach(Node::stop);
        testEndLatch.countDown();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (!testComplete()) {
            if(leader != null && evt instanceof StopEvent && ((StopEvent) evt).source.equals(leader.id())){
                leader = null;
            }

            if(evt instanceof StateTransitionEvent){
                StateTransitionEvent event = (StateTransitionEvent)evt;
                if(event.newState().isLeader()){
                    leader = event.oldState().node();
                }
            }
            if(evt instanceof Event){
                synchronized (this) {
                    check(source(ctx), (Event) evt);
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

    protected abstract void check(Node source, Event event);

    public Guarantee setup(){
        return this;
    }

    public Guarantee tearDown(){
        return this;
    }

    public boolean testComplete(){
        return testEndLatch.getCount() <= 0;
    }

    protected boolean isFromLeader(ChannelHandlerContext ctx){
        if(leader == null) return false;
        return source(ctx).isLeader();
    }

    protected Node source(ChannelHandlerContext ctx){
        return (Node)ctx.pipeline().context(Node.class).handler();
    }

}
