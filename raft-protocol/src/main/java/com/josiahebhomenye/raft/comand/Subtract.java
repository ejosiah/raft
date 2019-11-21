package com.josiahebhomenye.raft.comand;

public class Subtract extends Command {
    public Subtract(int value) {
        super(value);
    }

    @Override
    void apply(Data data) {
        data.value(data.value() - value);
    }

    @Override
    int id() {
        return SUBTRACT;
    }
}
