package com.josiahebhomenye.raft.comand;

public class Multiply extends Command {

    public Multiply(int value) {
        super(value);
    }

    @Override
    public void apply(Data data) {
        data.value(data.value() * value);
    }

    @Override
    public int id() {
        return MULTIPLY;
    }
}
