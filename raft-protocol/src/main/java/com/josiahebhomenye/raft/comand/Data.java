package com.josiahebhomenye.raft.comand;

import lombok.AllArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

@With
@lombok.Data
@Accessors(fluent = true)
@AllArgsConstructor
public class Data implements Cloneable{
    private int value;

    @Override
    public Data clone() throws CloneNotSupportedException {
        return (Data)super.clone();
    }
}