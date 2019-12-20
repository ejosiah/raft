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
    private long index;
    // FIXME don't send entry just send index, who ever needs it will have their own copy of the log
    private LogEntry entry;

    public ApplyEntryEvent(long index, LogEntry entry, SocketAddress source){
        super(source);
        this.index = index;
        this.entry = entry;
    }
}
