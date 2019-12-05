package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Command;
import lombok.Value;

@Value
public class LogEntry {
    private int term;
    private Command command;
}
