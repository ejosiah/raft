package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.RequestVote;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class RequestVoteEvent extends Event {

    public RequestVote requestVote;
    private Channel sender;

    public RequestVoteEvent(RequestVote requestVote, Channel sender) {
        super(requestVote.getCandidateId());
        this.requestVote = requestVote;
        this.sender = sender;
    }
}
