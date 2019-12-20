package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.UnhandledMessageEvent;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ChannelHandler.Sharable
public class ServerClientLogger extends ChannelInboundHandlerAdapter {

    private static final Logger log = LoggerFactory.getLogger("ServerClient");

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        log.debug("Server received connection from client {}", ctx.channel().remoteAddress());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        log.debug("client {} is inactive", ctx);
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ctx.fireChannelRead(msg);
    }


    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if(evt instanceof UnhandledMessageEvent){
            Object msg = ((UnhandledMessageEvent)evt).msg();
            log.warn("received msg {} from {} for which no handler was defined", msg, ctx.channel().remoteAddress());
        }
        ctx.fireUserEventTriggered(evt);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("something went wrong with connection from " + ctx.channel().remoteAddress(), cause);
        ctx.fireExceptionCaught(cause);
    }
}
