package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.RequestVoteReply;

public class RequestVoteReplyDecoderTest extends JsonDecoderTest<RequestVoteReply> {
    @Override
    protected RequestVoteReply createObjectToDecode() {
        return new RequestVoteReply(1, true);
    }

    @Override
    protected JsonDecoder<RequestVoteReply> decoder() {
        return new RequestVoteReplyeDecoder();
    }
}
