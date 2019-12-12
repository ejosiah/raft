package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.RequestVoteReply;
import io.netty.channel.ChannelHandlerContext;
import lombok.RequiredArgsConstructor;

import java.util.function.BiConsumer;


@RequiredArgsConstructor
public class RequestRequestVoteForCount implements BiConsumer<ChannelHandlerContext, RequestVote> {

    private final int rejectCount;
    private int count = 0;

    @Override
    public void accept(ChannelHandlerContext ctx, RequestVote requestVote) {
        if(count < rejectCount){
            ctx.writeAndFlush(new RequestVoteReply(requestVote.getTerm(), false));
        }else{
            ctx.writeAndFlush(new RequestVoteReply(requestVote.getTerm(), true));
        }
        count++;
    }
}
