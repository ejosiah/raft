package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.RequestVote;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class SendRequestVoteEvent extends Event {
    private RequestVote requestVote;

    public SendRequestVoteEvent(RequestVote requestVote){
        super(requestVote.getCandidateId());
        this.requestVote = requestVote;
    }
}
