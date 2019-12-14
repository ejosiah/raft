package com.josiahebhomenye.raft.server.util;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Getter
@Accessors(fluent = true)
public class RangeTimeout implements Timeout {

    private static final Random RNG = new Random();

    private final Duration lower;
    private final Duration upper;


    @Override
    public long get() {
        return RNG.nextInt((int)upper.minus(lower).toMillis()) + lower.toMillis();
    }

}
