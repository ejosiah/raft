package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.comand.Data;
import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import lombok.SneakyThrows;

public class DefaultStateManager extends StateManager<Data> {

    public DefaultStateManager(){
        super(new Data(0));
    }

    @Override
    @SneakyThrows
    Data handle(ApplyEntryEvent event) {
        Data temp = state.clone();
        event.entry().getCommand().apply(temp);
        state = temp;
        return temp;
    }
}
