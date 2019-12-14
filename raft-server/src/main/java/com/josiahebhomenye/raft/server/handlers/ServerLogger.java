package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.PeerConnectedEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@RequiredArgsConstructor
@Slf4j
public class ServerLogger extends ChannelDuplexHandler {
    
    private final Node node;

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        log.info("attempting to bind to {}", localAddress);
        promise.addListener(f -> {
            log.info("server bound to {}", localAddress);
        });
        super.bind(ctx, localAddress, promise);
    }


    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        log.info("disconnect from {} is currently in progress", ctx.channel().remoteAddress());
        promise.addListener( f -> {
            if(f.isSuccess()){
                log.info("disconnected from {}", ctx.channel().remoteAddress());
            }
        });
        super.disconnect(ctx, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("Something went wrong", cause);
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof StateTransitionEvent){
            StateTransitionEvent event = (StateTransitionEvent)evt;
            log.info("{} transition from {} state to {} state", node, event.oldState(), event.newState());
        }else if(evt instanceof PeerConnectedEvent){
            PeerConnectedEvent event = (PeerConnectedEvent)evt;
            log.info("{} connected to peer {}", node, event.getSource());
        }
        ctx.fireUserEventTriggered(evt);
    }
}
