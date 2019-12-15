package com.josiahebhomenye.raft.event;

import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.SocketAddress;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class ApplyEntryEvent extends Event {
    private LogEntry entry;

    public ApplyEntryEvent(LogEntry entry, SocketAddress source){
        super(source);
        this.entry = entry;
    }
}
