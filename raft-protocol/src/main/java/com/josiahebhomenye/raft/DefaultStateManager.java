package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.CommandDeserializer;
import com.josiahebhomenye.raft.comand.Data;
import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import lombok.SneakyThrows;

public class DefaultStateManager extends StateManager<Command, Data> {

    public DefaultStateManager(){
        super(new CommandDeserializer(), new Data(0));
    }

    @Override
    @SneakyThrows
    Data handle(ApplyEntryEvent event) {
        Data temp = state.clone();
        Command command = entryDeserializer.deserialize(event.entry().getCommand());
        command.apply(temp);
        state = temp;
        return temp;
    }
}
