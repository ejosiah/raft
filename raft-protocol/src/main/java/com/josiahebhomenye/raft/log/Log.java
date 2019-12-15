package com.josiahebhomenye.raft.log;

import lombok.SneakyThrows;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Log implements AutoCloseable{
    private static final int INT_SIZE = 4;
    public static final int LONG_SIZE = 8;
    private static final int COMMAND_SIZE = INT_SIZE * 2;
    private static final int SIZE_OFFSET = COMMAND_SIZE + LONG_SIZE;
    private RandomAccessFile data;

    @SneakyThrows
    public Log(String path){
        data = new RandomAccessFile(path, "rwd");
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
        if(index < 1 || index > size()) return null;
        byte[] buff = new byte[SIZE_OFFSET];
        data.seek(pos(index));
        data.read(buff);

        try {
            return LogEntry.deserialize(buff);
        } catch (Exception e) {
            return  null;
        }
    }

    private long pos(long index){
        return (index-1) * SIZE_OFFSET;
    }

    @Override
    @SneakyThrows
    public void close(){
        try(RandomAccessFile rdf = data){
            // use only to close
        }
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
    public Log clear(){
        data.setLength(0);
        return this;
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
    public List<LogEntry> entriesFrom(long index) {
        List<LogEntry> entries = new ArrayList<>();
        long lastIndex = getLastIndex();
        for(long i = index; i <= lastIndex; i++){
            entries.add(get(i));
        }
        return entries;
    }

    public boolean hasEntryAt(long index) {
        return getLastIndex() >= index;
    }

    @Override
    @SneakyThrows
    public boolean equals(Object o){
        if (this == o) return true;
        if (!(o instanceof Log)) return false;
        Log log = (Log) o;
        if(log.size() == 0) return false;
        if(log.size() != this.size()) return false;

        long lenght = data.length();
        data.seek(0);
        log.data.seek(0);
        for(long i = 0; i < lenght; i++){
            if(log.data.read() != data.read()) return false;
        }
        return true;
    }
}
