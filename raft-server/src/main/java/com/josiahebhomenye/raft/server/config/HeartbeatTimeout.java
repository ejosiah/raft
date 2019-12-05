package com.josiahebhomenye.raft.server.config;

import com.josiahebhomenye.raft.server.util.FixedTimeout;
import com.typesafe.config.Config;

import java.time.Duration;

public class HeartbeatTimeout extends FixedTimeout {

    public HeartbeatTimeout(Config config){
        this(config.getDuration("raft.timeout.heartbeat"));
    }

    public HeartbeatTimeout(Duration value) {
        super(value);
    }
}
