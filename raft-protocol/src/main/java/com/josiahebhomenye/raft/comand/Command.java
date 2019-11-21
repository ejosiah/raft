package com.josiahebhomenye.raft.comand;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class Command {

    protected final int value;

    public static final int SET = 1;
    public static final int ADD = 1 << 1;
    public static final int SUBTRACT = 1 << 2;
    public static final int MULTIPLY = 1 << 3;
    public static final int DIVIDE = 1 << 4;

    abstract void apply(Data data);

    abstract int id();

    public static Command restore(byte[] data){
        return null;
    }
}
