package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.RequestVoteReply;

public class RequestVoteReplyeDecoder extends JsonDecoder<RequestVoteReply> {
    public RequestVoteReplyeDecoder() {
        super(RequestVoteReply.class);
    }
}
