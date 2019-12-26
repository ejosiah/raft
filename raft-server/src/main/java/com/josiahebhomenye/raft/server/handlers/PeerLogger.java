package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.Peer;
import com.josiahebhomenye.raft.server.util.Dynamic;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@Slf4j
@ChannelHandler.Sharable
@RequiredArgsConstructor
public class PeerLogger extends ChannelDuplexHandler {

    private int retires = 0;
    private final Peer peer;

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        promise.addListener(f -> {
            if(!f.isSuccess()) {
                retires++;
                if(retires%50 == 0) {
                    log.debug("server unable to connected to peer {}", remoteAddress);
                    log.debug("will try to connect to peer {} again, retry count is {}", remoteAddress, retires);
                }
            }
        });
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        log.debug("disconnect from {} is currently in progress", ctx.channel().remoteAddress());
        promise.addListener( f -> {
            if(f.isSuccess()){
                log.debug("disconnected from {}", ctx.channel().remoteAddress());
            }
        });
        ctx.disconnect(promise);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Dynamic.invoke(this, "log", msg);
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(String.format("error [%s] caught on %s", cause.getMessage(), peer), cause);
        ctx.fireExceptionCaught(cause);
    }

    public void log(RequestVoteReply reply){
        if(reply.isVoteGranted()){
            log.info("Node[{}] granting vote to candidate[{}]", peer.node().name(), peer.name());
        }
    }

    public void log(AppendEntriesReply reply){
        if(!reply.isSuccess()){
            log.debug("Node[{}]'s log not consistent with leader[{}]'s log", peer.node().name(), peer.name());
        }
    }
}
