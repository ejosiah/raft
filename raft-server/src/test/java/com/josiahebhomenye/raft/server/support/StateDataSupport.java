package com.josiahebhomenye.raft.server.support;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;

public interface StateDataSupport {

    @SneakyThrows
    default void deleteState(){
        deleteState("log.dat", "state.dat");
    }

    @SneakyThrows
    default void deleteState(String logPath, String statePath){
        new File(logPath).delete();
        new File(statePath).delete();
    }
}
