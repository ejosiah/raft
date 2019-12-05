package com.josiahebhomenye.raft.server.util;

import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.util.Random;

@RequiredArgsConstructor
public class RangeTimeout implements Timeout {

    private static final Random RNG = new Random();

    private final Duration lower;
    private final Duration upper;


    @Override
    public long get() {
        return RNG.nextInt((int)upper.minus(lower).toMillis()) + lower.toMillis();
    }
}
