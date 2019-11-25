package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.RequestVote;

public class RequestVoteDecoder extends JsonDecoder<RequestVote> {

    public RequestVoteDecoder(){
        super(RequestVote.class);
    }
}
