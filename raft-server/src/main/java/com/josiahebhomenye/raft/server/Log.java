package com.josiahebhomenye.raft.server;

import com.josiahebhomenye.raft.comand.Command;
import lombok.SneakyThrows;
import java.io.RandomAccessFile;

public class Log {
    private static final int SIZE_OFFSET = Integer.SIZE * 2;
    private RandomAccessFile data;
    private int[] buffer = new int[SIZE_OFFSET];

    @SneakyThrows
    public Log(String path){
        data = new RandomAccessFile(path, "rw");
    }

    public int add(Command command){
        return 0;
    }

    public Command get(int id){
        return null;
    }
}
