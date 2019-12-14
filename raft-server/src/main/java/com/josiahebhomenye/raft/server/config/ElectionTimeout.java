package com.josiahebhomenye.raft.server.config;

import com.josiahebhomenye.raft.server.util.RangeTimeout;
import com.typesafe.config.Config;

import java.time.Duration;

public class ElectionTimeout extends RangeTimeout {

    public ElectionTimeout(Config config){
            super(config.getDuration("raft.timeout.election.lower")
                    , config.getDuration("raft.timeout.election.upper"));
    }

    public ElectionTimeout(Duration lower, Duration upper){
        super(lower, upper);
    }
}
