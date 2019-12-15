package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.RequestVoteReply;

public class RequestVoteReplyeDecoder extends JsonDecoder<RequestVoteReply> {
    public RequestVoteReplyeDecoder() {
        super(RequestVoteReply.class);
    }
}
