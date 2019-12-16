package com.josiahebhomenye.test.support;

import lombok.SneakyThrows;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
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
}
