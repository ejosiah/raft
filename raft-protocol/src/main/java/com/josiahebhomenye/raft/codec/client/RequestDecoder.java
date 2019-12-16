package com.josiahebhomenye.raft.codec.client;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.codec.JsonDecoder;

public class RequestDecoder extends JsonDecoder<Request> {
    public RequestDecoder() {
        super(Request.class);
    }
}
