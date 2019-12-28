package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import static com.josiahebhomenye.raft.server.core.NodeState.Id.LEADER;


@Slf4j
public class ForceLeader extends Interceptor {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        Node node = node(ctx);
        if(evt.equals(StateTransitionEvent.initialStateTransition())){
            log.info("forcing leader state on {}", node);
            node.state().transitionTo(LEADER);
            ctx.pipeline().remove(this);
            log.info("removed ForceLeader interceptor from pipeline");
        }else{
            ctx.fireUserEventTriggered(evt);
        }
    }
}
