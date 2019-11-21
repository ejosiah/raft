package com.josiahebhomenye.raft.comand;


public class Add extends Command {


    public Add(int value) {
        super(value);
    }

    @Override
    public void apply(Data data) {
        data.value(data.value() + value);
    }

    @Override
    int id() {
        return ADD;
    }
}
