package com.josiahebhomenye.raft.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josiahebhomenye.raft.comand.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.SneakyThrows;
import org.junit.Ignore;
import org.junit.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

public abstract class JsonDecoderTest<T> {
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void decodeBytesToExpectedObject(){
        EmbeddedChannel channel = new EmbeddedChannel(decoder());

        T expected = createObjectToDecode();

        ByteBuf buf = createBuffer(expected);


        assertTrue(channel.writeInbound(buf));
        assertTrue(channel.finish());

        assertEquals(channel.readInbound(), expected);
    }

    @Test
    public void decodePartialBytesToAppendEntries(){
        EmbeddedChannel channel = new EmbeddedChannel(decoder());

        T expected = createObjectToDecode();

        ByteBuf buf = createBuffer(expected);

        while(buf.readableBytes() > 3){
            ByteBuf tmp = Unpooled.buffer(3);
            buf.readBytes(tmp);

            assertFalse(channel.writeInbound(tmp));
        }

        assertTrue(channel.writeInbound(buf));
        assertTrue(channel.finish());

        assertEquals(channel.readInbound(), expected);
    }

    @Test
    public void skipDecoderWhenMessageTypeIsNotJson(){
        EmbeddedChannel channel = new EmbeddedChannel(decoder());

        ByteBuf buf = createBuffer("This is What we need to do");
        channel.writeInbound(buf);

        assertEquals(buf, channel.readInbound());
    }


    @Test
    public void skipDecokderWhenClassTypeOfMessageIsNotWithDecoderExpects(){
        EmbeddedChannel channel = new EmbeddedChannel(decoder());

        ByteBuf buf = Unpooled.buffer();
        buf.writeInt(MessageType.JSON.getValue());
        buf.writeInt(String.class.getName().length());
        buf.writeBytes(String.class.getName().getBytes());

        channel.writeInbound(buf);

        assertEquals(buf, channel.readInbound());
    }

    @Test
    public void decodeMultipleMessage(){
        EmbeddedChannel channel = new EmbeddedChannel(decoder());

        T expected = createObjectToDecode();

        ByteBuf buf = createBuffer(expected);
        buf.writeBytes(buf.copy());
        buf.writeBytes(buf.copy());


        assertTrue(channel.writeInbound(buf));

        assertEquals(channel.readInbound(), expected);
        assertEquals(channel.readInbound(), expected);
        assertEquals(channel.readInbound(), expected);
    }

    @SneakyThrows
    protected  <U> ByteBuf createBuffer(U object){
        ByteBuf buf = Unpooled.buffer();

        ByteArrayOutputStream bufOStream = new ByteArrayOutputStream();
        mapper.writeValue(bufOStream, object);
        byte[] json = bufOStream.toByteArray();

        String  clazz = object.getClass().getName();

        buf.writeInt(MessageType.JSON.getValue());
        buf.writeInt(clazz.length());
        buf.writeBytes(clazz.getBytes());
        buf.writeInt(json.length);
        buf.writeBytes(json);

        return buf;
    }

    protected abstract  T createObjectToDecode();

    protected abstract JsonDecoder<T> decoder();
}
