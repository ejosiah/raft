package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;
import org.omg.CORBA.TIMEOUT;

import static com.josiahebhomenye.raft.server.core.NodeState.*;
import java.util.List;

@RequiredArgsConstructor
public class PreElectionSetup extends Interceptor {
    private final List<RemotePeerMock> peers;

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt.equals(new StateTransitionEvent(NULL_STATE, FOLLOWER, node.getId()))){
            peers.forEach(RemotePeerMock::startClient);
        }
        ctx.fireUserEventTriggered(evt);
    }
}
