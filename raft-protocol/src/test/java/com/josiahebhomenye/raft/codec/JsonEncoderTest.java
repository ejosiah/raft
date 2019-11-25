package com.josiahebhomenye.raft.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public abstract class JsonEncoderTest<T> {

    private final ObjectMapper mapper = new ObjectMapper();


    @Test
    @SuppressWarnings("unchecked")
    public void encodeToJson() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel(encoder());

        T expected = createObjectToEncode();

        assertTrue(channel.writeOutbound(expected));

        ByteBuf buf = channel.readOutbound();


        buf.skipBytes(4);   // skip message type bytes
        buf.skipBytes(buf.readInt());   // skip class type bytes
        byte[] json = new byte[buf.readInt()];
        buf.readBytes(json);
        T actual = (T) mapper.readValue(json, expected.getClass());
        assertEquals(String.format("actual did not match expected: %s", expected.getClass().getSimpleName()), actual, expected);
    }


    protected abstract  T createObjectToEncode();

    protected abstract JsonEncoder<T> encoder();
}
