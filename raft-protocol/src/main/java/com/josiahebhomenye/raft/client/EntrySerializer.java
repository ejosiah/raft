package com.josiahebhomenye.raft.client;

public interface EntrySerializer<T> {

    byte[] serialize(T entry);
}
