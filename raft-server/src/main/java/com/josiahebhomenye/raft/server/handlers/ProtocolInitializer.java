package com.josiahebhomenye.raft.server.handlers;

import com.josiahebhomenye.raft.codec.*;
import com.josiahebhomenye.raft.codec.client.RequestDecoder;
import com.josiahebhomenye.raft.codec.client.RequestEncoder;
import com.josiahebhomenye.raft.codec.client.ResponseDecoder;
import com.josiahebhomenye.raft.codec.client.ResponseEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;

@ChannelHandler.Sharable
public class ProtocolInitializer<C extends Channel> extends ChannelInitializer<C> {

    @Override
    protected void initChannel(C ch) throws Exception {
        ch.pipeline()
            .addLast(new JsonCodec<>(new RedirectDecoder(), new RedirectEncoder()))
            .addLast(new JsonCodec<>(new ResponseDecoder(), new ResponseEncoder()))
            .addLast(new JsonCodec<>(new RequestDecoder(), new RequestEncoder()))
            .addLast(new JsonCodec<>(new AppendEntriesDecoder(), new AppendEntriesEncoder()))
            .addLast(new JsonCodec<>(new AppendEntriesReplyDecoder(), new AppendEntriesReplyEncoder()))
            .addLast(new JsonCodec<>(new RequestVoteDecoder(), new RequestVoteEncoder()))
            .addLast(new JsonCodec<>(new RequestVoteReplyeDecoder(), new RequestvoteReplyEncoder()));
    }
}
