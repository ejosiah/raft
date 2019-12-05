package com.josiahebhomenye.raft.server.util;

import lombok.RequiredArgsConstructor;

import java.time.Duration;

@RequiredArgsConstructor
public class FixedTimeout implements Timeout {

    private final Duration value;

    @Override
    public long get() {
        return value.toMillis();
    }
}
