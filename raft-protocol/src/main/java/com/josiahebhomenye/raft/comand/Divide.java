package com.josiahebhomenye.raft.comand;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Data;

public class Divide extends Command {

    public Divide(int value) {
        super(value);
    }

    @Override
    public void apply(Data data) {
        data.value(data.value()/value);
    }

    @Override
    public int id() {
        return DIVIDE;
    }
}
