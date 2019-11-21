package com.josiahebhomenye.raft.comand;

import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

@lombok.Data
@Accessors(fluent = true)
@AllArgsConstructor
public class Data {
    private int value;
}