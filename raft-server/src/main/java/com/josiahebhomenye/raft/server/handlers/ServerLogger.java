package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.PeerConnectedEvent;
import com.josiahebhomenye.raft.server.event.PeerDisconnectedEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ServerLogger extends ChannelDuplexHandler {

    private static final Logger log = LoggerFactory.getLogger("NodeServer");

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
        }else if(evt instanceof PeerDisconnectedEvent){
            PeerDisconnectedEvent event = (PeerDisconnectedEvent)evt;
            log.info("peer {} disconnect from server {}", event.getSource(), node.id());
        }else if(evt instanceof AppendEntriesEvent){
            AppendEntriesEvent event = (AppendEntriesEvent)evt;
            event.msg().getEntries().forEach(entry -> {
                LogEntry logEntry = LogEntry.deserialize(entry);
                if(Command.type(logEntry.getCommand()) != 0){
                    log.debug("appending log entry {} from {}", logEntry, event.source);
                }else{
                    log.warn("received ill formatted log entry {} from {}", logEntry, event.source);
                }
            });
        }
        ctx.fireUserEventTriggered(evt);
    }
}
