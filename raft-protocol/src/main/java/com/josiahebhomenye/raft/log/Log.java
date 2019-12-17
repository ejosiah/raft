package com.josiahebhomenye.raft.log;

import lombok.SneakyThrows;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Log implements AutoCloseable, Iterable<LogEntry>{
    private static final int INT_SIZE = 4;
    public static final int LONG_SIZE = 8;
    private RandomAccessFile data;
    private int entrySize;
    private int sizeOffset;

    @SneakyThrows
    public Log(String path, int entrySize){
        data = new RandomAccessFile(path, "rwd");
        this.entrySize = entrySize;
        this.sizeOffset = entrySize + LONG_SIZE;
    }

    @SneakyThrows
    public void add(LogEntry entry, long index){
        if(index < 1) throw new IllegalArgumentException("index: " + index + ", cannot less than one");
        if(entry == null) throw new IllegalArgumentException("command cannot be null");
        data.seek((index - 1) * sizeOffset);

        data.write(entry.serialize());
    }

    public void add(LogEntry entry){
        add(entry, getLastIndex() + 1);
    }

    @SneakyThrows
    public LogEntry get(long index){
        if(index < 1 || index > size()) return null;
        byte[] buff = new byte[sizeOffset];
        data.seek(pos(index));
        data.read(buff);

        try {
            return LogEntry.deserialize(buff);
        } catch (Exception e) {
            return  null;
        }
    }

    private long pos(long index){
        return (index-1) * sizeOffset;
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
        return ((data.length() - sizeOffset)/sizeOffset ) + 1;
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
        long newSize = (i - 1) * sizeOffset;
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

    @Override
    @SneakyThrows
    public Iterator<LogEntry> iterator() {
        return new Iterator<LogEntry>() {
            final long size = size();
            long nextIndex = 0;
            @Override
            public boolean hasNext() {
                return nextIndex < size;
            }

            @Override
            public LogEntry next() {
                return get(++nextIndex);
            }
        };
    }
}
