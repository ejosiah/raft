package com.josiahebhomenye.raft.server.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerClientLogger extends ChannelInboundHandlerAdapter {

    private final Logger logger = LoggerFactory.getLogger("Node");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("Server received connection from client {}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.warn("client {} is inactive", ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("something went wrong with connection from " + ctx.channel().remoteAddress(), cause);
        ctx.fireExceptionCaught(cause);
    }
}
