package com.josiahebhomenye.raft.client.handlers;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;

@Slf4j
@ChannelHandler.Sharable
public class ClientLogger extends ChannelDuplexHandler {

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        promise.addListener(f -> {
            if(f.isSuccess()){
                log.info("successfully connected to {}",  remoteAddress);
            }else{
                log.info("unable to connect to {}", remoteAddress);
            }
        });
        super.connect(ctx, remoteAddress, localAddress, promise);
    }
}
