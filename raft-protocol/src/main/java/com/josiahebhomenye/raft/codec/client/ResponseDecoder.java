package com.josiahebhomenye.raft.codec.client;

import com.josiahebhomenye.raft.client.Response;
import com.josiahebhomenye.raft.codec.JsonDecoder;

public class ResponseDecoder extends JsonDecoder<Response> {
    public ResponseDecoder() {
        super(Response.class);
    }
}
