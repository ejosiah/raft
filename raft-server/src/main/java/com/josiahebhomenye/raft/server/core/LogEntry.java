package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Command;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Value;

import java.nio.ByteBuffer;

@Value
public class LogEntry {
    private int term;
    private Command command;

    public byte[] serialize(){
        ByteBuf buf = Unpooled.buffer();
        byte[] command = this.command.serialize();
        buf.writeInt(term);
        buf.writeBytes(command);

        byte[] content = new byte[buf.readableBytes()];
        buf.readBytes(content);

        return content;
    }

    public static LogEntry deserialize(byte[] entry){
        ByteBuf buf = Unpooled.wrappedBuffer(entry);

        int term = buf.readInt();
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        Command command = Command.restore(data);
        return new LogEntry(term, command);
    }
}
