package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.server.core.Peer;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
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
            if(f.isSuccess()) {
                log.info("server connected to peer {}", remoteAddress);
            }else{
                retires++;
                if(retires%50 == 0) {
                    log.info("server unable to connected to peer {}", remoteAddress);
                    log.info("will try to connect to peer {} again, retry count is {}", remoteAddress, retires);
                }
            }
        });
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        log.info("disconnect from {} is currently in progress", ctx.channel().remoteAddress());
        promise.addListener( f -> {
            if(f.isSuccess()){
                log.info("disconnected from {}", ctx.channel().remoteAddress());
            }
        });
        ctx.disconnect(promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error(String.format("error [%s] caught on %s", cause.getMessage(), peer), cause);
        ctx.fireExceptionCaught(cause);
    }
}
