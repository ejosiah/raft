package com.josiahebhomenye.raft.codec.client;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.codec.JsonDecoder;
import com.josiahebhomenye.raft.codec.JsonDecoderTest;

import java.util.Random;
import java.util.UUID;

public class RequestDecoderTest extends JsonDecoderTest<Request> {
    @Override
    protected Request createObjectToDecode() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        return new Request(UUID.randomUUID().toString(), data);
    }

    @Override
    protected JsonDecoder<Request> decoder() {
        return new RequestDecoder();
    }
}
