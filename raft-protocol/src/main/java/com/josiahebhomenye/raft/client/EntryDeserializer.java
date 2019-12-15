package com.josiahebhomenye.raft.client;

public interface EntryDeserializer<T> {

    int entrySize();

    T deserialize(byte[] entry);
}
