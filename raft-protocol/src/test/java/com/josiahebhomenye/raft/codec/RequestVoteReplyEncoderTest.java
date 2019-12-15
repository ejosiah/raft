package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.RequestVoteReply;

public class RequestVoteReplyEncoderTest extends JsonEncoderTest<RequestVoteReply>{
    @Override
    protected RequestVoteReply createObjectToEncode() {
        return new RequestVoteReply(1, true);
    }

    @Override
    protected JsonEncoder<RequestVoteReply> encoder() {
        return new RequestvoteReplyEncoder();
    }
}
