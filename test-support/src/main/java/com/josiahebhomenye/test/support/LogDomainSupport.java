package com.josiahebhomenye.test.support;

import com.josiahebhomenye.raft.comand.Divide;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Multiply;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.comand.Subtract;
import com.josiahebhomenye.raft.log.LogEntry;

import java.util.LinkedList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

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

    default LinkedList<LogEntry> leaderEntries(){
        return new LinkedList<LogEntry>(){
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
    
    default LinkedList<LogEntry> followerMissingEntries0(){
        return leaderEntries().stream().limit(9).collect(Collectors.toCollection(LinkedList::new));
    }    
    
    default LinkedList<LogEntry> followerMissingEntries1(){
        return  leaderEntries().stream().limit(4).collect(Collectors.toCollection(LinkedList::new));
    }

    default LinkedList<LogEntry> followerWithExtraUnCommittedEntries0(){
        LinkedList<LogEntry> entries = leaderEntries();
        entries.add(new LogEntry(6, new Divide(8).serialize()));
        return entries;
    }

    default LinkedList<LogEntry> followerWithExtraUnCommittedEntries1(){
        LinkedList<LogEntry> entries = leaderEntries();
        entries.add(new LogEntry(7, new Add(8).serialize()));
        entries.add(new LogEntry(7, new Multiply(2).serialize()));
        return entries;
    }

    default LinkedList<LogEntry> followerWithMissingAndExtraUnCommittedEntries0(){
        LinkedList<LogEntry> entries = leaderEntries().stream().limit(5).collect(Collectors.toCollection(LinkedList::new));
        entries.add(new LogEntry(4, new Subtract(2).serialize()));
        entries.add(new LogEntry(4, new Subtract(4).serialize()));
        return entries;
    }

    default LinkedList<LogEntry> followerWithMissingAndExtraUnCommittedEntries1(){
        LinkedList<LogEntry> entries = leaderEntries().stream().limit(3).collect(Collectors.toCollection(LinkedList::new));
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
