package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.server.event.ElectionTimeoutEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteEvent;

public class Leader extends NodeState {

    @Override
    public void init() {

    }

    @Override
    public void handle(ElectionTimeoutEvent event) {

    }

    @Override
    public void handle(RequestVoteEvent event) {

    }
}
