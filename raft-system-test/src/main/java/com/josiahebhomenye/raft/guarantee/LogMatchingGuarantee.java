package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;
import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
public class LogMatchingGuarantee extends Guarantee {

    public LogMatchingGuarantee(List<Node> nodes, CountDownLatch testEndLatch) {
        super(nodes, testEndLatch);
    }

    @Override
    protected void check(ChannelHandlerContext ctx, Event event) {

    }
}
