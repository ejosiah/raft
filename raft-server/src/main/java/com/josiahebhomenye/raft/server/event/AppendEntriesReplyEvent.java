package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Peer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

@Data
@With
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class AppendEntriesReplyEvent extends Event {

    private AppendEntriesReply msg;
    private Peer sender;

    public AppendEntriesReplyEvent(AppendEntriesReply msg, Peer sender){
        super(sender.channel());
        this.msg = msg;
        this.sender = sender;
    }
}
