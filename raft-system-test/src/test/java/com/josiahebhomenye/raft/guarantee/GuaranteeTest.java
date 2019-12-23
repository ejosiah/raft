package com.josiahebhomenye.raft.guarantee;

import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.BindEvent;
import com.josiahebhomenye.raft.server.util.CheckedExceptionWrapper;
import com.josiahebhomenye.test.support.EventFilter;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;

public  abstract class GuaranteeTest implements CheckedExceptionWrapper, StateDataSupport {

    Guarantee guarantee;
    CountDownLatch latch;
    LinkedList<Node> nodes;
    EventFilter eventFilter;
    EmbeddedChannel clientChannel;
    ServerConfig config;

    @Before
    public void setup0(){
        IntStream.range(0, 4).forEach(i -> {
            delete(String.format("log%s.dat", i));
            delete(String.format("state%s.dat", i));
        });
        config = new ServerConfig(ConfigFactory.load());
        nodes = new LinkedList<>();
        nodes.add(new Node(config.withId(new InetSocketAddress(9000)).withLogPath("log0.dat").withStatePath("state0.dat")));
        nodes.add(new Node(config.withId(new InetSocketAddress(9001)).withLogPath("log1.dat").withStatePath("state1.dat")));
        nodes.add(new Node(config.withId(new InetSocketAddress(9002)).withLogPath("log2.dat").withStatePath("state2.dat")));
        nodes.add(new Node(config.withId(new InetSocketAddress(9003)).withLogPath("log3.dat").withStatePath("state3.dat")));



        latch = new CountDownLatch(1);

        eventFilter = EventFilter.BLOACK_TIMEOUT_EVENTS_FILTER;
        guarantee = guarantee(nodes, latch);
        nodes.forEach(node -> {
            EmbeddedChannel channel = new EmbeddedChannel(eventFilter, node, guarantee);
            channel.pipeline().fireUserEventTriggered(new BindEvent(channel));
        });

        clientChannel = new EmbeddedChannel();

        guarantee.setup();
    }

    @After
    public void tearDown0(){
        guarantee.tearDown();
        nodes.forEach( node -> uncheck(() -> node.stop().get() ));
        IntStream.range(0, 4).forEach(i -> {
            delete(String.format("log%s.dat", i));
            delete(String.format("state%s.dat", i));
        });

    }

    protected abstract Guarantee guarantee(List<Node> nodes, CountDownLatch latch);
}
