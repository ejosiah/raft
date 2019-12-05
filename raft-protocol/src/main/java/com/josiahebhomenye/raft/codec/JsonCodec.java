package com.josiahebhomenye.raft.codec;

import io.netty.channel.CombinedChannelDuplexHandler;

public class JsonCodec<T> extends CombinedChannelDuplexHandler<JsonDecoder<T>, JsonEncoder<T>> {

    public JsonCodec(JsonDecoder<T> decoder, JsonEncoder<T> encoder){
        super(decoder, encoder);
    }
}
