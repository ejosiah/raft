package com.josiahebhomenye.raft.server.support;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Data;
import com.josiahebhomenye.raft.event.StateUpdatedEvent;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.core.Node;
import com.josiahebhomenye.raft.server.event.ReceivedRequestEvent;
import com.josiahebhomenye.raft.server.util.CheckedExceptionWrapper;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public abstract class RaftScenarios implements StateDataSupport, CheckedExceptionWrapper {

    CountDownLatch leaderStartLatch;
    CountDownLatch testEndLatch;
    LeaderStart leaderStart;
    TestEnd testEnd;
    List<Node> nodes = new ArrayList<>();
    Data data = new Data(0);
    List<NodeState> nodeStates = nodeStates();
    InetSocketAddress leaderId;
    AtomicBoolean running = new AtomicBoolean(false);
    EmbeddedChannel channel;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Before
    public void setup(){
        leaderStartLatch = new CountDownLatch(1);
        testEndLatch = new CountDownLatch(nodeStates.size() - 1);
        leaderStart = new LeaderStart(leaderStartLatch);

        data = new Data(0);

        NodeState leaderState = nodeStates.stream().filter(ns -> ns.leader()).findFirst().get();
        leaderId = leaderState.id();
        List<LogEntry> entries = new ArrayList<>(leaderState.logEntries());
        entries.addAll(newEntries());

        entries.stream().map(le -> Command.restore(le.getCommand())).forEach(command -> {
            command.apply(data);
        });

        testEnd = new TestEnd(new StateUpdatedEvent(data, null), testEndLatch);
        channel = new EmbeddedChannel();

        buildStateData();
        buildLogEntries();
        constructNodes();
        running.set(true);
    }

    void buildStateData(){
        IntStream.range(0, nodeStates.size()).forEach(i -> {
            NodeState nodeState = nodeStates.get(i);
            writeState(nodeState.currentTerm(), nodeState.id(), String.format("state%s.dat", i));
        });
    }

    void buildLogEntries(){
        IntStream.range(0, nodeStates.size()).forEach(i -> {
            try(Log log = new Log(String.format("log%s.dat", i), 8)){
                List<LogEntry> entries =  nodeStates.get(i).logEntries();

                IntStream.range(0, entries.size()).forEach(j -> {
                    log.add(entries.get(j), j+1);
                });
            }
        });
    }

    void constructNodes(){
        IntStream.range(0, nodeStates.size()).forEach(i -> {
            NodeState nodeState = nodeStates.get(i);
            ServerConfig config = new ServerConfig(ConfigFactory.load());
            String statePath = String.format("state%s.dat", i);
            String logPath = String.format("log%s.dat", i);
            // List<InetSocketAddress> peers = nodeStates.stream().map(ns -> nodeState.id()).filter(id -> !id.equals(nodeState.id())).collect(Collectors.toList()); TODO why is this not working
            List<InetSocketAddress> peers = new ArrayList<>();
            for(int j = 0; j < nodeStates.size(); j++){
                if(!nodeStates.get(j).equals(nodeState)){
                    peers.add(nodeStates.get(j).id());
                }
            }
            config = config.withStatePath(statePath).withLogPath(logPath).withPeers(peers).withId(nodeState.id());
            Node node = new Node(config);
            if(nodeState.leader()){
                node.addPreProcessInterceptors(new ForceLeader());
            }else {
                node.addPreProcessInterceptors(leaderStart);
                node.addPostProcessInterceptors(testEnd);
            }
            nodes.add(node);
        });
    }

    @After
    public void tearDown(){
        nodes.forEach(node -> {
            wrap(() ->node.stop().get());
            delete(node.config().logPath);
            delete(node.config().statePath);
        });
        running.set(false);
        nodes.clear();
    }

    @Test
    public void runScenario() throws Exception {
        Node leader = nodes.stream().filter(n -> n.id().equals(leaderId)).findFirst().orElseThrow(() -> new Exception("No Leader defined"));

        new NewEntriesSupplier(leader).start();

        leader.start();
        leaderStartLatch.countDown();

        List<Node> followers = nodes.stream().filter(n -> !n.id().equals(leaderId)).collect(Collectors.toList());
        followers.forEach(Node::start);

        if(!testEndLatch.await(30, TimeUnit.SECONDS)){
            fail(String.format("%s did not finish before timeout elapsed", this.getClass().getSimpleName()));
        }
        Thread.sleep(2000);  // wait a little bit for logs to flush to disk

        followers.forEach( follower -> {
            assertEquals(String.format("follower %s log not in sync with leader's", follower.id()), leader.log(), follower.log());
        });

    }

    protected List<LogEntry> newEntries(){
        return Collections.emptyList();
    }

    protected abstract List<NodeState> nodeStates();

    @RequiredArgsConstructor
    class NewEntriesSupplier extends Thread{

        private final Node leader;
        Random random = new Random();
        Iterator<LogEntry> itr = newEntries().iterator();

        @Override
        @SneakyThrows
        public void run() {
            leaderStartLatch.await();
            while(running.get() && itr.hasNext()){
                Thread.sleep(random.nextInt(100));
                ReceivedRequestEvent event = new ReceivedRequestEvent(new Request(itr.next().getCommand()), channel);
                leader.channel().pipeline().fireUserEventTriggered(event);
            }
        }
    }
}
