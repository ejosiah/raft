package com.josiahebhomenye.raft.codec;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Set;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.Test;

import java.net.InetSocketAddress;

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