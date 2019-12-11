package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Command;
import lombok.SneakyThrows;
import java.io.RandomAccessFile;

public class Log {
    private static final int BYTE_SIZE = 4;
    private static final int COMMAND_SIZE = BYTE_SIZE  * 2;
    private static final int SIZE_OFFSET = BYTE_SIZE * 3;
    private RandomAccessFile data;

    @SneakyThrows
    public Log(String path){
        data = new RandomAccessFile(path, "rw");
    }

    @SneakyThrows
    public void add(LogEntry entry, long index){
        if(index < 1) throw new IllegalArgumentException("index: " + index + ", cannot less than one");
        if(entry == null) throw new IllegalArgumentException("command cannot be null");
        data.seek((index - 1) * SIZE_OFFSET);
        data.writeInt(entry.getTerm());
        data.writeInt(entry.getCommand().id());
        data.writeInt(entry.getCommand().getValue());
    }

    @SneakyThrows
    public LogEntry get(long index){
        if(index < 1) throw new IllegalArgumentException("index: " + index + ", cannot less than one");

        byte[] buff = new byte[COMMAND_SIZE];
        data.seek((index-1) * SIZE_OFFSET);

        int term = data.readInt();
        data.read(buff);
        Command command =  Command.restore(buff);
        return new LogEntry(term, command);
    }

    @SneakyThrows
    public void close(){
        data.close();
    }

    @SneakyThrows
    public boolean isEmpty(){
        return data.length() == 0;
    }

    @SneakyThrows
    public LogEntry lastEntry(){
        if(data.length() == 0) return null;
        return get(getLastIndex());
    }

    @SneakyThrows
    public long getLastIndex() {
        return ((data.length() - SIZE_OFFSET)/SIZE_OFFSET ) + 1;
    }

    @SneakyThrows
    public void clear(){
        data.setLength(0);
    }

    public long size() {
        return getLastIndex();
    }

    @SneakyThrows
    public void deleteFromIndex(long i) {
        if(i > getLastIndex()) return;
        long newSize = (i - 1) * SIZE_OFFSET;
        data.setLength(newSize);
    }
}
