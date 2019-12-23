package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.AppendEntriesReply;

public class AppendEntriesReplyEncoderTest extends JsonEncoderTest<AppendEntriesReply> {
    @Override
    protected AppendEntriesReply createObjectToEncode() {
        return new AppendEntriesReply(1, 0, true);
    }

    @Override
    protected JsonEncoder<AppendEntriesReply> encoder() {
        return new AppendEntriesReplyEncoder();
    }
}
