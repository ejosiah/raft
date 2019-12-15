package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.rpc.AppendEntries;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import static org.junit.Assert.*;

public class AppendEntriesDecoderTest extends JsonDecoderTest<AppendEntries> implements AppendEntriesTestFactory{


    @Test
    public void decodeAppendEntriesWithNoEntries(){
        EmbeddedChannel channel = new EmbeddedChannel(decoder());


        AppendEntries expected = getHeartBeat();

        ByteBuf buf = createBuffer(expected);


        assertTrue(channel.writeInbound(buf));
        assertTrue(channel.finish());

        assertEquals(channel.readInbound(), expected);
    }

    @Override
    protected AppendEntries createObjectToDecode() {
        return get();
    }

    @Override
    protected JsonDecoder<AppendEntries> decoder() {
        return new AppendEntriesDecoder();
    }
}