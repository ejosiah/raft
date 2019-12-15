package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.RequestVote;

import java.net.InetSocketAddress;

public class RequestVoteEncoderTest extends JsonEncoderTest<RequestVote> {
    @Override
    protected RequestVote createObjectToEncode() {
        return new RequestVote(1, 2, 3, new InetSocketAddress("localhost", 8080));
    }

    @Override
    protected JsonEncoder<RequestVote> encoder() {
        return new RequestVoteEncoder();
    }
}
