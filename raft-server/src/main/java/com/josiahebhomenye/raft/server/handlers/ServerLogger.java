package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.raft.server.util.Dynamic;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class ServerLogger extends Interceptor {

    private static final Logger log = LoggerFactory.getLogger("NodeServer");

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
        log.debug("disconnect from {} is currently in progress", ctx.channel().remoteAddress());
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

    public void log(Node node, StateTransitionEvent event){
        if(event.newState().isFollower()){
            log.info("Node[{}] is now a follower", node.id());
        }else if(event.newState().isCandidate()){
            log.info("Node[{}] is now a candidate and in contention to be leader", node.id());
        }else if(event.newState().isLeader()){
            log.info("Node[{}] is now leader for term {}", node.id(), node.currentTerm());
        }
    }

    public void log(Node node, PeerConnectedEvent event){
        log.info("Node[{}] connected to Peer[{}]", node.id(), event.getSource().remoteAddress());
    }

    public void log(Node node, PeerDisconnectedEvent event){
        log.info("Node[{}] disconnected from Peer[{}]", node.id(), event.getSource().remoteAddress());
    }

    public void log(Node node, ConnectPeerEvent event){
        log.info("Node[{}] connecting to Peer[{}]", node.id(), event.getSource().remoteAddress());
    }

    public void log(Node node, AppendEntriesEvent event){
        event.msg().getEntries().forEach(entry -> {
            LogEntry logEntry = LogEntry.deserialize(entry);
            if(Command.type(logEntry.getCommand()) != 0){
                log.debug("appending log entry {} from {}", logEntry, event.source);
            }else{
                log.warn("received ill formatted log entry {} from {}", logEntry, event.source);
            }
        });
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        Dynamic.invoke(this, "log", node(ctx), evt);
        ctx.fireUserEventTriggered(evt);
    }
}
