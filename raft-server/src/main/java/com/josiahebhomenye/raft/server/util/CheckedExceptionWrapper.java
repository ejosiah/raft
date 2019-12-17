package com.josiahebhomenye.raft.server.util;

import lombok.SneakyThrows;

import java.util.function.Supplier;

public interface CheckedExceptionWrapper {

    interface CheckedRunnable{
        void run() throws Exception;
    }

    @SneakyThrows
    default void wrap(CheckedRunnable runnable){
        runnable.run();
    }
}
