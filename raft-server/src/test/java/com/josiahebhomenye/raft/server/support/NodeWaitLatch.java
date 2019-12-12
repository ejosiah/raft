package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Setter
@AllArgsConstructor
public class NodeWaitLatch extends Interceptor {

    @Setter
    private CountDownLatch latch;
    private BiPredicate<Node, Object> trigger;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(trigger.test(node, evt)){
            latch.countDown();
        }
        ctx.fireUserEventTriggered(evt);
    }
}
