package com.josiahebhomenye.raft.codec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.josiahebhomenye.raft.comand.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.util.List;

@RequiredArgsConstructor
public class JsonDecoder<T> extends ByteToMessageDecoder {

    enum State{
        MESSAGE_TYPE, CLASS_TYPE_LENGTH, CLASS_TYPE, JSON_LENGTH, READ_JSON
    }

    protected final ObjectMapper mapper = new ObjectMapper();
    private final Class<T> clazz;
    private State state = State.MESSAGE_TYPE;
    private int length = 0;

    @Override
    @SuppressWarnings("unchecked")
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

        if(state == State.MESSAGE_TYPE){
            if(in.readableBytes() < 4 ) return;

            in.markReaderIndex();

            int type = in.readInt();
            if(type != MessageType.JSON.getValue()){
                in.resetReaderIndex();
                reset();
                ctx.fireChannelRead(in.retain());
                return;
            }
            state = State.CLASS_TYPE_LENGTH;

        }

        if(state == State.CLASS_TYPE_LENGTH){
            if(in.readableBytes() < 4) return;

            length = in.readInt();
            state = State.CLASS_TYPE;
        }

        if(state == State.CLASS_TYPE){
            if(in.readableBytes() < length) return;
            byte[] buf = new byte[length];
            in.readBytes(buf);
            Class<T> sClaszz = (Class<T>) Class.forName(new String(buf));

            if(sClaszz != clazz){
                in.resetReaderIndex();
                reset();
                ctx.fireChannelRead(in.retain());
                return;
            }
            state = State.JSON_LENGTH;

        }

        if(state == State.JSON_LENGTH){
            if(in.readableBytes() < 4) return;
            length = in.readInt();
            state = State.READ_JSON;
        }


        if(in.readableBytes() < length) return;

        byte[] buf  = new byte[length];
        in.readBytes(buf);
        T msg = mapper.readValue(buf, clazz);
        out.add(msg);
        reset();
    }

    void reset(){
        state = State.MESSAGE_TYPE;
        length = 0;
    }
}
