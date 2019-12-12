package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.RequestVote;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

public class RequestVoteDecoder extends JsonDecoder<RequestVote> {

    public RequestVoteDecoder(){
        super(RequestVote.class);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        super.decode(ctx, in, out);
    }
}
