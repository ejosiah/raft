package com.josiahebhomenye.raft.comand;

public class Subtract extends Command {
    public Subtract(int value) {
        super(value);
    }

    @Override
    public void apply(Data data) {
        data.value(data.value() - value);
    }

    @Override
    public int id() {
        return SUBTRACT;
    }
}
