package com.josiahebhomenye.raft.server.event;

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
public class SendRequestVoteEvent extends Event {
    private RequestVote requestVote;

    public SendRequestVoteEvent(RequestVote requestVote, Channel source){
        super(source);
        this.requestVote = requestVote;
    }
}
