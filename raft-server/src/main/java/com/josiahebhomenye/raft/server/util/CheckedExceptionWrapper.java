package com.josiahebhomenye.raft.server.util;

import lombok.SneakyThrows;

public interface CheckedExceptionWrapper {

    interface CheckedCallable<T>{
        T call() throws Exception;
    }

    interface CheckedRunnable{
        void run() throws Exception;
    }

    @SneakyThrows
    default <T> T uncheck(CheckedCallable<T> runnable){
        return runnable.call();
    }

    @SneakyThrows
    default void uncheckVoid(CheckedRunnable runnable){
        runnable.run();
    }

}
