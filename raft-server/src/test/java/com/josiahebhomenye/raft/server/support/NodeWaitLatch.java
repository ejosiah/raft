package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;

@Setter
@AllArgsConstructor
public class NodeWaitLatch extends Interceptor {

    @Setter
    private CountDownLatch testWaitLatch;
    private CountDownLatch nodeWaitLatch;
    private BiPredicate<Node, Object> trigger;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(trigger.test(node, evt)){
            testWaitLatch.countDown();
            nodeWaitLatch.await();
        }
        ctx.fireUserEventTriggered(evt);
    }
}
