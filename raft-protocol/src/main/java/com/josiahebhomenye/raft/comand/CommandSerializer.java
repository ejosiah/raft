package com.josiahebhomenye.raft.comand;

import com.josiahebhomenye.raft.client.EntrySerializer;

public class CommandSerializer implements EntrySerializer<Command> {
    @Override
    public byte[] serialize(Command entry) {
        return entry.serialize();
    }
}
