package com.josiahebhomenye.raft.log;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Value;

@Value
public class LogEntry {
    private long term;
    private byte[] command;

    public byte[] serialize(){
        ByteBuf buf = Unpooled.buffer();
        buf.writeLong(term);
        buf.writeBytes(command);

        byte[] content = new byte[buf.readableBytes()];
        buf.readBytes(content);

        return content;
    }

    public static LogEntry deserialize(byte[] entry){
        ByteBuf buf = Unpooled.wrappedBuffer(entry);

        long term = buf.readLong();
        byte[] command = new byte[buf.readableBytes()];
        buf.readBytes(command);
        return new LogEntry(term, command);
    }
}
