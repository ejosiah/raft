package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.server.core.Node;

import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LeaderAppendOnlyGuaranteeTest extends GuaranteeTest {
    @Override
    protected Guarantee guarantee(List<Node> nodes, CountDownLatch latch) {
        return new LeaderAppendOnlyGuarantee(nodes, latch);
    }
}
