package com.josiahebhomenye.test.support;

import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import lombok.SneakyThrows;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public interface StateDataSupport {

    default String logPath(){ return  UUID.randomUUID().toString(); };
    default String statePath(){ return UUID.randomUUID().toString(); }

    @SneakyThrows
    default void deleteAllState(){
        delete("log.dat");
        delete( "state.dat");
    }

    @SneakyThrows
    default void delete(String path){
        try {
            Files.delete(new File(path).getAbsoluteFile().toPath());
        } catch (NoSuchFileException e) {
            // Ignore
        }
    }

    default void writeState(long term, InetSocketAddress votedFor, String path){
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream(path))){
            out.writeLong(term);

            if(votedFor != null) {
                out.writeUTF(votedFor.getHostName());
                out.writeInt(votedFor.getPort());
            }
        }catch(Exception ex){

        }
    }

    default void writeLog(List<LogEntry> entries, String path){
        try(Log log = new Log(path, 8)){
            entries.forEach(log::add);
        }
    }
}
