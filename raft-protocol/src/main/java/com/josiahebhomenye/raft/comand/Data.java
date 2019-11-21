package com.josiahebhomenye.raft.comand;

import lombok.experimental.Accessors;

@lombok.Data
@Accessors(fluent = true)
public class Data {
    private int value;
}