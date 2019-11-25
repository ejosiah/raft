package com.josiahebhomenye.raft.server;

import com.josiahebhomenye.raft.comand.Command;
import lombok.SneakyThrows;
import java.io.RandomAccessFile;

public class Log {
    private static final int BYTE_SIZE = 4;
    private static final int SIZE_OFFSET = BYTE_SIZE * 2;
    private RandomAccessFile data;
    private int[] buffer = new int[SIZE_OFFSET];

    @SneakyThrows
    public Log(String path){
        data = new RandomAccessFile(path, "rw");
    }

    @SneakyThrows
    public void add(Command command, long id){
        if(id < 0) throw new IllegalArgumentException("index: " + id + ", cannot be negative");
        if(command == null) throw new IllegalArgumentException("command cannot be null");
        data.seek(id * SIZE_OFFSET);
        data.writeInt(command.id());
        data.writeInt(command.getValue());
    }

    @SneakyThrows
    public Command get(long id){
        byte[] buff = new byte[SIZE_OFFSET];
        data.seek(id * SIZE_OFFSET);
        data.read(buff);
        return Command.restore(buff);
    }

    @SneakyThrows
    public void close(){
        data.close();
    }
}
