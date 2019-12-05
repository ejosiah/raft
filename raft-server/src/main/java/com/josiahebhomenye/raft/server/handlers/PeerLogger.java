package com.josiahebhomenye.raft.server.handlers;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@ChannelHandler.Sharable
public class PeerLogger extends ChannelDuplexHandler {

    private final Logger logger = LoggerFactory.getLogger("Node");
    private int retires = 0;

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
        promise.addListener(f -> {
            if(f.isSuccess()) {
                logger.info("server connected to peer {}", remoteAddress);
            }else{
                retires++;
                logger.info("server unable to connected to peer {}", remoteAddress);
                logger.info("will try to connect to peer {} again, retry count is {}", retires, remoteAddress);
            }
        });
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) {
        logger.info("disconnect from {} is currently in progress", ctx.channel().remoteAddress());
        promise.addListener( f -> {
            if(f.isSuccess()){
                logger.info("disconnected from {}", ctx.channel().remoteAddress());
            }
        });
        ctx.disconnect(promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Something went wrong", cause);
        ctx.fireExceptionCaught(cause);
    }
}
