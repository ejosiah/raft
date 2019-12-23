package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.InboundEvent;
import com.josiahebhomenye.raft.rpc.RequestVote;
import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class RequestVoteEvent extends Event implements InboundEvent {

    public RequestVote requestVote;
    private Channel sender;

    public RequestVoteEvent(RequestVote requestVote, Channel sender) {
        super(sender);
        this.requestVote = requestVote;
        this.sender = sender;
    }
}
