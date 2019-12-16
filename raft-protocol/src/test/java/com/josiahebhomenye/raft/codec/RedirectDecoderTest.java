package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.codec.client.RequestEncoderTest;
import com.josiahebhomenye.raft.rpc.Redirect;

import java.net.InetSocketAddress;

public class RedirectDecoderTest extends JsonDecoderTest<Redirect> {
    @Override
    protected Redirect createObjectToDecode() {
        Request request = new RequestEncoderTest().createObjectToEncode();
        return new Redirect(new InetSocketAddress(9000), request);
    }

    @Override
    protected JsonDecoder<Redirect> decoder() {
        return new RedirectDecoder();
    }
}
