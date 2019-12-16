package com.josiahebhomenye.raft.codec.client;

import com.josiahebhomenye.raft.client.Response;
import com.josiahebhomenye.raft.codec.JsonEncoder;
import com.josiahebhomenye.raft.codec.JsonEncoderTest;

import java.util.Random;
import java.util.UUID;

public class ResponseEncoderTest extends JsonEncoderTest<Response> {
    @Override
    protected Response createObjectToEncode() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        return new Response(UUID.randomUUID().toString(),UUID.randomUUID().toString(), true, data);
    }

    @Override
    protected JsonEncoder<Response> encoder() {
        return new ResponseEncoder();
    }
}
