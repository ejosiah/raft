package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.Redirect;

public class RedirectDecoder extends JsonDecoder<Redirect> {
    public RedirectDecoder() {
        super(Redirect.class);
    }
}
