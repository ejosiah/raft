package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.RequestVote;

import java.net.InetSocketAddress;

public class RequestVoteDecoderTest extends JsonDecoderTest<RequestVote> {
    @Override
    protected RequestVote createObjectToDecode() {
        return new RequestVote(1, 2, 3, new InetSocketAddress("localhost", 8080));
    }

    @Override
    protected JsonDecoder<RequestVote> decoder() {
        return new RequestVoteDecoder();
    }
}
