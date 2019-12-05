package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.RequestVote;
import io.netty.channel.Channel;
import lombok.Value;
import lombok.experimental.Accessors;

@Value
@Accessors(fluent = true)
public class RequestVoteEvent extends Event {

    public final RequestVote requestVote;
    private final Channel sender;

    public RequestVoteEvent(RequestVote requestVote, Channel sender) {
        super(requestVote.getCandidateId());
        this.requestVote = requestVote;
        this.sender = sender;
    }
}
