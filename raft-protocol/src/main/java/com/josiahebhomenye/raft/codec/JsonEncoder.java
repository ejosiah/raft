package com.josiahebhomenye.raft.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josiahebhomenye.raft.comand.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayOutputStream;

@RequiredArgsConstructor
public class JsonEncoder<T> extends MessageToByteEncoder<T> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void encode(ChannelHandlerContext ctx, T msg, ByteBuf out) throws Exception {
        ByteArrayOutputStream bufOStream = new ByteArrayOutputStream();
        mapper.writeValue(bufOStream, msg);
        byte[] json = bufOStream.toByteArray();

        String  clazz = msg.getClass().getName();

        out.writeInt(MessageType.JSON.getValue());
        out.writeInt(clazz.getBytes().length);
        out.writeBytes(clazz.getBytes());
        out.writeInt(json.length);
        out.writeBytes(json);
    }
}
