package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.rpc.RpcMessage;
import com.josiahebhomenye.raft.server.core.Node;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.RequiredArgsConstructor;

@ChannelHandler.Sharable
@RequiredArgsConstructor
public class Enricher extends ChannelDuplexHandler {

    final Node node;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if(msg instanceof RpcMessage){
            ctx.writeAndFlush(((RpcMessage)msg).setSenderId(node.id()));
        }else {
            super.write(ctx, msg, promise);
        }
    }
}

