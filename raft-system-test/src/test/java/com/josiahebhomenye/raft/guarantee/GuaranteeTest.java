package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.BindEvent;
import com.josiahebhomenye.raft.server.util.CheckedExceptionWrapper;
import com.josiahebhomenye.test.support.EventFilter;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public  abstract class GuaranteeTest implements CheckedExceptionWrapper {

    Guarantee guarantee;
    CountDownLatch latch;
    LinkedList<Node> nodes;
    EventFilter eventFilter;

    @Before
    public void setup(){
        ServerConfig config = new ServerConfig(ConfigFactory.load());
        nodes = new LinkedList<>();
        nodes.add(new Node(config));
        nodes.add(new Node(config));
        nodes.add(new Node(config));
        nodes.add(new Node(config));


        latch = new CountDownLatch(1);

        eventFilter = EventFilter.BLOACK_TIMEOUT_EVENTS_FILTER;
        guarantee = guarantee(nodes, latch);
        nodes.forEach(node -> {
            EmbeddedChannel channel = new EmbeddedChannel(eventFilter, node, guarantee);
            channel.pipeline().fireUserEventTriggered(new BindEvent(channel));
        });
    }

    @After
    public void tearDown(){
        nodes.forEach( node -> uncheck(() -> node.stop().get() ));
    }

    protected abstract Guarantee guarantee(List<Node> nodes, CountDownLatch latch);
}
