package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.server.core.Interceptor;
import com.josiahebhomenye.raft.server.core.Node;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

import java.util.LinkedList;
import java.util.concurrent.CountDownLatch;

@Accessors(fluent = true)
@RequiredArgsConstructor
public abstract class Guarantee extends Interceptor {

    private final LinkedList<Node> nodes;
    private final CountDownLatch testEndLatch;

    @Getter
    private boolean passed;

    public void fail(){
        passed = true;
        nodes.forEach(Node::stop);
        testEndLatch.countDown();
    }
}
