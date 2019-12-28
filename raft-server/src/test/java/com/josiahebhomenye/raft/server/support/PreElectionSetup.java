package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.josiahebhomenye.raft.server.core.NodeState.Id.FOLLOWER;
import static com.josiahebhomenye.raft.server.core.NodeState.Id.NOTHING;

@RequiredArgsConstructor
public class PreElectionSetup extends Interceptor {
    private final List<RemotePeerMock> peers;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if(evt.equals(new StateTransitionEvent(NOTHING, FOLLOWER, node(ctx)))){
            peers.forEach(RemotePeerMock::startClient);
        }
        ctx.fireUserEventTriggered(evt);
    }
}
