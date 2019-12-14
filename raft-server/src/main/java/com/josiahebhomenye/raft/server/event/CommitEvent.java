package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

@Data
//@With
//@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class CommitEvent extends Event {
    private long index;

    public CommitEvent(){

    }

    public CommitEvent(long index, InetSocketAddress source){
        super(source);
        this.index = index;
    }
}
