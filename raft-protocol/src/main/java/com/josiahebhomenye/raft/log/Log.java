package com.josiahebhomenye.raft.log;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.RandomAccessFile;
import java.util.*;

@Slf4j
public class Log implements AutoCloseable, Iterable<LogEntry>, Cloneable{
    private static final int INT_SIZE = 4;
    public static final int LONG_SIZE = 8;
    private static final int KILO_BYTE = 1024;
    private static final int MEGA_BYTE = KILO_BYTE * KILO_BYTE;
    public static final int END_OF_FILE = -1;
    private RandomAccessFile data;
    private int entrySize;
    private int sizeOffset;
    private String path;

    @SneakyThrows
    public Log(String path, int entrySize){
        this.path = path;
        this.entrySize = entrySize;
        data = new RandomAccessFile(path, "rw");
        this.entrySize = entrySize;
        this.sizeOffset = entrySize + LONG_SIZE;
    }

    @SneakyThrows
    public void add(LogEntry entry, long index){
        log.debug("adding {} at index {}", entry, index);
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

        Log thisLog = clone();
        Log otherLog = log.clone();

        try {
            if(otherLog.size() == 0) return false;
            if(thisLog.size() != otherLog.size()) return false;

            long length = thisLog.data.length();
            thisLog.data.seek(0);
            otherLog.data.seek(0);

            byte[] thisBuf = new byte[Math.min((int)length, MEGA_BYTE)];    // FIXME truncation of long due to cast
            byte[] otherBuf = new byte[Math.min((int)length, MEGA_BYTE)];    // FIXME truncation of long due to cast

            while(true){
                int read = thisLog.data.read(thisBuf);
                otherLog.data.read(otherBuf);

                if(read == END_OF_FILE) break;

                if(!Arrays.equals(thisBuf, otherBuf)) return false;
            }
            return true;
        } finally {
            thisLog.close();
            otherLog.close();
        }
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

    @Override
    public Log clone() {
        return new Log(path, entrySize);
    }

    @SneakyThrows
    public void copy(Log log){

        try(Log otherLog = log.clone()){
            data.setLength(0);
            data.seek(0);
            otherLog.data.seek(0);

            long length = otherLog.data.length();
            byte[] buf = new byte[Math.min((int)length, MEGA_BYTE)];
            int read;

            while((read = otherLog.data.read(buf)) != END_OF_FILE){
                data.write(buf, 0, read);
            }
        }
    }
}
