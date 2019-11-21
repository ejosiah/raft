package com.josiahebhomenye.raft.comand;

public class Set extends Command {

    public Set(int value) {
        super(value);
    }

    @Override
    public void apply(Data data) {
        data.value(value);
    }

    @Override
    int id() {
        return SET;
    }
}
