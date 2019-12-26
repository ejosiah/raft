package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.nio.channels.ClosedChannelException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Accessors(fluent = true)
@RequiredArgsConstructor
@ChannelHandler.Sharable
public class FailOnExceptionHandler extends ChannelDuplexHandler {

    protected final List<Node> nodes;
    protected final CountDownLatch testEndLatch;

    @Getter
    private Throwable cause;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(!(cause instanceof ClosedChannelException)) {
            synchronized (this) {
                this.cause = cause;
                nodes.forEach(Node::stop);
                testEndLatch.countDown();
            }
        }
    }
}
