package com.josiahebhomenye.raft.comand;


import lombok.EqualsAndHashCode;

public class Add extends Command {


    public Add(int value) {
        super(value);
    }

    @Override
    public void apply(Data data) {
        data.value(data.value() + value);
    }

    @Override
    public int id() {
        return ADD;
    }
}
