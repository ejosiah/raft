package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.rpc.RequestVote;
import com.josiahebhomenye.raft.event.Event;
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

    public SendRequestVoteEvent(RequestVote requestVote){
        super(requestVote.getCandidateId());
        this.requestVote = requestVote;
    }
}
