package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.client.EntryGenerator;
import com.josiahebhomenye.raft.client.NodeKiller;
import com.josiahebhomenye.raft.guarantee.*;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.core.NodeState;
import com.josiahebhomenye.raft.server.util.CheckedExceptionWrapper;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.ChannelDuplexHandler;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static java.lang.String.*;
import static org.junit.Assert.*;

@Slf4j
public class SystemTest implements CheckedExceptionWrapper, StateDataSupport {

    private static final Duration RUNTIME = Duration.of(5, ChronoUnit.MINUTES);

    EntryGenerator entryGenerator;
    CountDownLatch testLatch;
    Semaphore guard;
    ServerConfig config;
    List<Node> nodes = new ArrayList<>();
    List<Guarantee> guarantees = new ArrayList<>();
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);
    NodeKiller nodeKiller;

    @Before
    public void setup(){
        delete("append_only_log_check.log");

        config = new ServerConfig(ConfigFactory.load());
        testLatch = new CountDownLatch(1);
        guard = new Semaphore(1);

        guarantees.add(new ElectionSafetyGuarantee(nodes, testLatch, config.majority).setup());
        guarantees.add(new LeaderAppendOnlyGuarantee(nodes, testLatch).setup());
        guarantees.add(new LogMatchingGuarantee(nodes, testLatch).setup());
        guarantees.add(new LeaderCompletenessGuarantee(nodes, testLatch).setup());
        guarantees.add(new StateMachineSafetyGuarantee(nodes, testLatch).setup());

        IntStream.range(0, 5).forEach(i -> {
            ServerConfig serverConfig = new ServerConfig(ConfigFactory.load());
            InetSocketAddress id = new InetSocketAddress( "localhost", 9000+i);
            if(i > 0){
                List<InetSocketAddress> peers = new ArrayList<>(serverConfig.peers);
                peers.remove(id);
                peers.add(serverConfig.id);
                serverConfig = serverConfig.withPeers(peers);
            }
            serverConfig = serverConfig.withLogPath(format("log%s.dat", i)).withStatePath(format("state%s.dat", i)).withId(id);
            delete(serverConfig.logPath);
            delete(serverConfig.statePath);
            Node node = new Node(serverConfig);
            node.addPostProcessInterceptors(new ArrayList<>(guarantees));
            nodes.add(node);
        });

        nodes.forEach(node -> {
            assertFalse(format("%s can not have more that 4 peer servers", node), node.config().peers.size() > 4);
            assertFalse(format("%s cannot be a included in it's peers", node), node.config().peers.stream().anyMatch(id -> id.equals(node.id())));
        });

        nodeKiller = new NodeKiller(nodes, testLatch);
        entryGenerator = new EntryGenerator(RUNTIME, testLatch, nodeKiller);
    }

    @After
    public void tearDown(){
        executor.shutdownNow();
        nodes.forEach(node -> {
            uncheck(() -> node.stop().get());
        });
        nodes.forEach(node -> {
            delete(node.config().statePath);
            delete(node.config().logPath);
        });
        guarantees.forEach(Guarantee::tearDown);

        nodes.forEach(node -> {
            log.info("end of test state: {}", node);
        });
        guarantees.forEach(guarantee ->{
            if(guarantee.passed()) {
                log.info("{} passed", guarantee.getClass().getSimpleName());
            }else {
                log.info("{} failed", guarantee.getClass().getSimpleName());
            }
        });
    }

    @Test
    public void validate_that_the_system_meets_the_raft_guarantees() throws Exception{

        nodes.forEach(node -> {
            try(Log log = node.log().clone()){
                assertEquals("logs should be initially empty", 0, log.size());
            }
        });

        nodes.forEach(node -> uncheckVoid(node::start) );

        executor.execute(nodeKiller);

        executor.schedule(() -> {

            nodes.forEach(node -> {
                node.channel().eventLoop().execute(() -> {
                    guarantees.forEach(guarantee -> {
                        if(node.state().equals(NodeState.LEADER())){
                            guarantee.leader(node);
                        }
                        node.channel().pipeline().addLast(guarantee);
                        node.channel().pipeline().addLast(nodeKiller);
                    });
                });
            });

            executor.execute(entryGenerator);
        }, 30, TimeUnit.SECONDS);

        long timeout = RUNTIME.toMillis() + (long)(RUNTIME.toMillis() * 0.25);
        testLatch.await(timeout, TimeUnit.MILLISECONDS);

        guarantees.forEach(guarantee -> {
            String name = guarantee.getClass().getSimpleName();
            assertTrue(format("%s was not met", name), guarantee.passed());
        });
    }
}
