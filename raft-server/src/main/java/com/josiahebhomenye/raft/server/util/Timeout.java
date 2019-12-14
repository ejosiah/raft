package com.josiahebhomenye.raft.server.util;

import java.util.concurrent.TimeUnit;

public interface Timeout {

    long get();

    default TimeUnit unit(){
        return TimeUnit.MILLISECONDS;
    }
}
