package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Command;
import lombok.SneakyThrows;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

public class Log {
    private static final int INT_SIZE = 4;
    public static final int LONG_SIZE = 8;
    private static final int COMMAND_SIZE = INT_SIZE * 2;
    private static final int SIZE_OFFSET = COMMAND_SIZE + LONG_SIZE;
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

        data.write(entry.serialize());
    }

    @SneakyThrows
    public LogEntry get(long index){
        if(index < 1) throw new IllegalArgumentException("index: " + index + ", cannot less than one");

        byte[] buff = new byte[SIZE_OFFSET];
        data.seek(pos(index));
        data.read(buff);

        return LogEntry.deserialize(buff);
    }

    private long pos(long index){
        return (index-1) * SIZE_OFFSET;
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

    // TODO optimize and get entries as byte array
    public List<LogEntry> entriesFrom(long lastApplied) {
        List<LogEntry> entries = new ArrayList<>();
        long lastIndex = getLastIndex();
        for(long i = lastApplied; i <= lastIndex; i++){
            entries.add(get(i));
        }
        return entries;
    }

    public boolean hasEntryAt(long index) {
        return getLastIndex() >= index;
    }
}
