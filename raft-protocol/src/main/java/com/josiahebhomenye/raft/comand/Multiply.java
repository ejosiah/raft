package com.josiahebhomenye.raft.comand;

public class Multiply extends Command {

    public Multiply(int value) {
        super(value);
    }

    @Override
    void apply(Data data) {
        data.value(data.value() * value);
    }

    @Override
    int id() {
        return MULTIPLY;
    }
}
