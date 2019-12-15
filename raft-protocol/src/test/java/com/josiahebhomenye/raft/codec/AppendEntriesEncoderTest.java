package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.AppendEntries;

public class AppendEntriesEncoderTest extends JsonEncoderTest<AppendEntries> implements AppendEntriesTestFactory{

    @Override
    protected AppendEntries createObjectToEncode() {
        return get();
    }

    @Override
    protected JsonEncoder<AppendEntries> encoder() {
        return new AppendEntriesEncoder();
    }
}