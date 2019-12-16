package com.josiahebhomenye.raft.client.support;

import com.josiahebhomenye.raft.client.EntrySerializer;

public class StringEntrySerializer implements EntrySerializer<String> {

    public static final StringEntrySerializer INSTANCE = new StringEntrySerializer();

    @Override
    public byte[] serialize(String entry) {
        return entry.getBytes();
    }
}
