package com.josiahebhomenye.raft.codec.client;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.codec.JsonEncoder;
import com.josiahebhomenye.raft.codec.JsonEncoderTest;

import java.util.Random;
import java.util.UUID;

public class RequestEncoderTest extends JsonEncoderTest<Request> {
    @Override
    public Request createObjectToEncode() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        return new Request(UUID.randomUUID().toString(), data);
    }

    @Override
    protected JsonEncoder<Request> encoder() {
        return new RequestEncoder();
    }
}
