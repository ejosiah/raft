package com.josiahebhomenye.raft.codec.client;

import com.josiahebhomenye.raft.client.Response;
import com.josiahebhomenye.raft.codec.JsonDecoder;
import com.josiahebhomenye.raft.codec.JsonDecoderTest;

import java.util.Random;
import java.util.UUID;

public class ResponseDecoderTest extends JsonDecoderTest<Response> {
    @Override
    protected Response createObjectToDecode() {
        byte[] data = new byte[1024];
        new Random().nextBytes(data);
        return new Response(UUID.randomUUID().toString(),UUID.randomUUID().toString(), true, data);
    }

    @Override
    protected JsonDecoder<Response> decoder() {
        return new ResponseDecoder();
    }
}
