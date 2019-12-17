package com.josiahebhomenye.test.support;

import com.josiahebhomenye.raft.comand.Divide;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Multiply;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.comand.Subtract;
import com.josiahebhomenye.raft.log.LogEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface LogDomainSupport {

    default List<LogEntry> logEntries(){
        return
            new ArrayList<LogEntry>(){
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

    default List<LogEntry> leaderEntries(){
        return new ArrayList<LogEntry>(){
            {
                add(new LogEntry(1, new Set(2).serialize()));
                add(new LogEntry(1, new Add(5).serialize()));
                add(new LogEntry(1, new Subtract(1).serialize()));
                add(new LogEntry(4, new Set(0).serialize()));
                add(new LogEntry(4, new Set(3).serialize()));
                add(new LogEntry(5, new Multiply(8).serialize()));
                add(new LogEntry(5, new Add(1).serialize()));
                add(new LogEntry(6, new Add(2).serialize()));
                add(new LogEntry(6, new Divide(3).serialize()));
                add(new LogEntry(6, new Add(4).serialize()));
            }
        };
    }
    
    default List<LogEntry> followerMissingEntries0(){
        return leaderEntries().stream().limit(9).collect(Collectors.toList());
    }    
    
    default List<LogEntry> followerMissingEntries1(){
        return  leaderEntries().stream().limit(4).collect(Collectors.toList());
    }

    default List<LogEntry> followerWithExtraUnCommittedEntries0(){
        List<LogEntry> entries = leaderEntries();
        entries.add(new LogEntry(6, new Divide(8).serialize()));
        return entries;
    }

    default List<LogEntry> followerWithExtraUnCommittedEntries1(){
        List<LogEntry> entries = leaderEntries();
        entries.add(new LogEntry(7, new Add(8).serialize()));
        entries.add(new LogEntry(7, new Multiply(2).serialize()));
        return entries;
    }

    default List<LogEntry> followerWithMissingAndExtraUnCommittedEntries0(){
        List<LogEntry> entries = leaderEntries().stream().limit(5).collect(Collectors.toList());
        entries.add(new LogEntry(4, new Subtract(2).serialize()));
        entries.add(new LogEntry(4, new Subtract(4).serialize()));
        return entries;
    }

    default List<LogEntry> followerWithMissingAndExtraUnCommittedEntries1(){
        List<LogEntry> entries = leaderEntries().stream().limit(3).collect(Collectors.toList());
        entries.add(new LogEntry(2, new Set(25).serialize()));
        entries.add(new LogEntry(2, new Add(5).serialize()));
        entries.add(new LogEntry(2, new Divide(3).serialize()));
        entries.add(new LogEntry(3, new Multiply(8).serialize()));
        entries.add(new LogEntry(3, new Subtract(8).serialize()));
        entries.add(new LogEntry(3, new Divide(12).serialize()));
        entries.add(new LogEntry(3, new Add(4).serialize()));
        entries.add(new LogEntry(3, new Add(15).serialize()));
        return entries;
    }
}
