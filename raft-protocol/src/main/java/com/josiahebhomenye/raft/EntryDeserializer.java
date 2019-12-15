package com.josiahebhomenye.raft;

public interface EntryDeserializer<T> {

    int entrySize();

    T deserialize(byte[] entry);
}
