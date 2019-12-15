package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.AppendEntriesReply;

public class AppendEntriesReplyDecoderTest extends JsonDecoderTest<AppendEntriesReply> {

    @Override
    protected AppendEntriesReply createObjectToDecode() {
        return new AppendEntriesReply(1, true);
    }

    @Override
    protected JsonDecoder<AppendEntriesReply> decoder() {
        return new AppendEntriesReplyDecoder();
    }
}
