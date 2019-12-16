package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.codec.client.RequestEncoderTest;
import com.josiahebhomenye.raft.rpc.Redirect;

import java.net.InetSocketAddress;

public class RedirectEncoderTest extends JsonEncoderTest<Redirect> {
    @Override
    protected Redirect createObjectToEncode() {
        Request request = new RequestEncoderTest().createObjectToEncode();
        return new Redirect(new InetSocketAddress(9000), request);
    }

    @Override
    protected JsonEncoder<Redirect> encoder() {
        return new RedirectEncoder();
    }
}
