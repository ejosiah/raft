package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.codec.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;

public class ProtocolInitializer<C extends Channel> extends ChannelInitializer<C> {

    @Override
    protected void initChannel(C ch) throws Exception {
        ch.pipeline()
                .addLast(new JsonCodec<>(new AppendEntriesDecoder(), new AppendEntriesEncoder()))
                .addLast(new JsonCodec<>(new AppendEntriesReplyDecoder(), new AppendEntriesReplyEncoder()))
                .addLast(new JsonCodec<>(new RequestVoteDecoder(), new RequestVoteEncoder()))
                .addLast(new JsonCodec<>(new RequestVoteReplyeDecoder(), new RequestvoteReplyEncoder()));
    }
}
