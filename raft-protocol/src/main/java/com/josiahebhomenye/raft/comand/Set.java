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
    public int id() {
        return SET;
    }
}
