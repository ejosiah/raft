package com.josiahebhomenye.raft.client.handlers;

import com.josiahebhomenye.raft.client.RaftClient;
import com.josiahebhomenye.raft.rpc.Redirect;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

@ChannelHandler.Sharable
public class ClientLogger extends ChannelDuplexHandler {

    private static Logger log = LoggerFactory.getLogger(RaftClient.class);

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        promise.addListener(f -> {
            if(f.isSuccess()){
                log.debug("successfully connected to {}",  remoteAddress);
            }else{
                log.debug("unable to connect to {}", remoteAddress);
            }
        });
        super.connect(ctx, remoteAddress, localAddress, promise);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("received msg {} from {}", msg, ctx.channel().remoteAddress());
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.debug("attempting to send message {} from {} to {}", msg, ctx.channel().localAddress(), ctx.channel().remoteAddress());
        super.write(ctx, msg, promise);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("system error: ", cause);
    }
}
