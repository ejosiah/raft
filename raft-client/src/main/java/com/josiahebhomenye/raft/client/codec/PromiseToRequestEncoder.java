package com.josiahebhomenye.raft.client.codec;

import com.josiahebhomenye.raft.client.PromiseRequest;
import com.josiahebhomenye.raft.client.Request;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

@ChannelHandler.Sharable
public class PromiseToRequestEncoder extends MessageToMessageEncoder<PromiseRequest> {

    @Override
    protected void encode(ChannelHandlerContext ctx, PromiseRequest promise, List<Object> out) throws Exception {
        Request request = new Request(promise.getId(), promise.getRequest().array());
        out.add(request);
    }

}
