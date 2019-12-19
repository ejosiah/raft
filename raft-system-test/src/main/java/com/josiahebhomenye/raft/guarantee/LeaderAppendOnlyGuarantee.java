package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.event.Event;
import com.josiahebhomenye.raft.server.core.Node;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LeaderAppendOnlyGuarantee extends Guarantee {

    public LeaderAppendOnlyGuarantee(List<Node> nodes, CountDownLatch latch) {
        super(nodes, latch);
    }

    @Override
    protected void check(Event event) {
        
    }
}
