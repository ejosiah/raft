package com.josiahebhomenye.raft.comand;

public class Divide extends Command {
    public Divide(int value) {
        super(value);
    }

    @Override
    void apply(Data data) {
        data.value(data.value()/value);
    }

    @Override
    int id() {
        return DIVIDE;
    }
}
