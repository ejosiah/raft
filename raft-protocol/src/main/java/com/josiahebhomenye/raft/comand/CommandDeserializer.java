package com.josiahebhomenye.raft.comand;

import com.josiahebhomenye.raft.client.EntryDeserializer;

public class CommandDeserializer implements EntryDeserializer<Command> {

    @Override
    public int entrySize() {
        return 8;
    }

    @Override
    public Command deserialize(byte[] entry) {
        return Command.restore(entry);
    }
}
