package com.josiahebhomenye.test.support;

import com.josiahebhomenye.raft.comand.Divide;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Multiply;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.comand.Subtract;
import com.josiahebhomenye.raft.log.LogEntry;

import java.util.LinkedList;

public interface LogDomainSupport {

    default LinkedList<LogEntry> logEntries(){
        return
            new LinkedList<LogEntry>(){
                {
                    add(new LogEntry(1, new Set(0).serialize()));
                    add(new LogEntry(1, new Add(5).serialize()));
                    add(new LogEntry(1, new Add(3).serialize()));
                    add(new LogEntry(1, new Subtract(1).serialize()));
                    add(new LogEntry(1, new Multiply(10).serialize()));
                    add(new LogEntry(1, new Divide(2).serialize()));
                }
            };
    }
}
