package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.ElectionTimeoutEvent;
import com.josiahebhomenye.raft.server.event.PeerConnectedEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@RequiredArgsConstructor
public class ServerLogger extends ChannelDuplexHandler {

    private final Logger logger = LoggerFactory.getLogger("Node");

    private final Node node;

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        logger.info("attempting to bind to {}", localAddress);
        promise.addListener(f -> {
            logger.info("server bound to {}", localAddress);
        });
        super.bind(ctx, localAddress, promise);
    }


    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        logger.info("disconnect from {} is currently in progress", ctx.channel().remoteAddress());
        promise.addListener( f -> {
            if(f.isSuccess()){
                logger.info("disconnected from {}", ctx.channel().remoteAddress());
            }
        });
        super.disconnect(ctx, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Something went wrong", cause);
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof StateTransitionEvent){
            StateTransitionEvent event = (StateTransitionEvent)evt;
            logger.info("Node: {} transition from {} state to {} state", node.getId(), event.oldState(), event.newState());
        }else if(evt instanceof PeerConnectedEvent){
            PeerConnectedEvent event = (PeerConnectedEvent)evt;
            logger.info("Node: {} connected to peer {}", node.getId(), event.getSource());
        }
        ctx.fireUserEventTriggered(evt);
    }
}
