package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.CommitEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.josiahebhomenye.raft.server.support.ForceLeader;
import com.josiahebhomenye.raft.server.support.LeaderStart;
import com.josiahebhomenye.raft.server.support.TestEnd;
import com.josiahebhomenye.raft.server.util.CheckedExceptionWrapper;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LogReplicationTest implements StateDataSupport, CheckedExceptionWrapper {

    ServerConfig config = new ServerConfig(ConfigFactory.load());
    Node leader;
    Node follower0;
    Node follower1;
    Node follower2;
    Node follower3;
    CountDownLatch leaderStartLatch;
    CountDownLatch testEndLatch;
    LeaderStart leaderStart;
    TestEnd testEnd;
    List<Node> nodes = new ArrayList<>();


    @Before
    public void setup(){
        leaderStartLatch = new CountDownLatch(1);
        testEndLatch = new CountDownLatch(4);
        leaderStart = new LeaderStart(leaderStartLatch);
        testEnd = new TestEnd(new CommitEvent(8L, null), testEndLatch);

        buildLogEntries();
        constructStateData();
        constructNodes();
    }

    void buildLogEntries(){
        Log leaderLog = new Log("log.dat", 8).clear();
        Log follower0Log = new Log("log0.dat", 8).clear();
        Log follower1Log = new Log("log1.dat", 8).clear();
        Log follower2Log = new Log("log2.dat", 8).clear();
        Log follower3Log = new Log("log3.dat", 8).clear();

        leaderLog.add(new LogEntry(1, new Set(3).serialize()), 1);
        leaderLog.add(new LogEntry(1, new Set(1).serialize()), 2);
        leaderLog.add(new LogEntry(1, new Set(9).serialize()), 3);
        leaderLog.add(new LogEntry(2, new Set(2).serialize()), 4);
        leaderLog.add(new LogEntry(3, new Set(0).serialize()), 5);
        leaderLog.add(new LogEntry(3, new Set(7).serialize()), 6);
        leaderLog.add(new LogEntry(3, new Set(5).serialize()), 7);
        leaderLog.add(new LogEntry(3, new Set(4).serialize()), 8);

        follower0Log.add(new LogEntry(1, new Set(3).serialize()), 1);
        follower0Log.add(new LogEntry(1, new Set(1).serialize()), 2);
        follower0Log.add(new LogEntry(1, new Set(9).serialize()), 3);
        follower0Log.add(new LogEntry(2, new Set(2).serialize()), 4);
        follower0Log.add(new LogEntry(3, new Set(0).serialize()), 5);


        follower1Log.add(new LogEntry(1, new Set(3).serialize()), 1);
        follower1Log.add(new LogEntry(1, new Set(1).serialize()), 2);
        follower1Log.add(new LogEntry(1, new Set(9).serialize()), 3);
        follower1Log.add(new LogEntry(2, new Set(2).serialize()), 4);
        follower1Log.add(new LogEntry(3, new Set(0).serialize()), 5);
        follower1Log.add(new LogEntry(3, new Set(7).serialize()), 6);
        follower1Log.add(new LogEntry(3, new Set(5).serialize()), 7);
        follower1Log.add(new LogEntry(3, new Set(4).serialize()), 8);

        follower2Log.add(new LogEntry(1, new Set(3).serialize()), 1);
        follower2Log.add(new LogEntry(1, new Set(1).serialize()), 2);

        follower3Log.add(new LogEntry(1, new Set(3).serialize()), 1);
        follower3Log.add(new LogEntry(1, new Set(1).serialize()), 2);
        follower3Log.add(new LogEntry(1, new Set(9).serialize()), 3);
        follower3Log.add(new LogEntry(2, new Set(2).serialize()), 4);
        follower3Log.add(new LogEntry(3, new Set(0).serialize()), 5);
        follower3Log.add(new LogEntry(3, new Set(7).serialize()), 6);
        follower3Log.add(new LogEntry(3, new Set(5).serialize()), 7);

        leaderLog.close();
        follower0Log.close();
        follower1Log.close();
        follower2Log.close();
        follower3Log.close();
    }

    void constructStateData(){
        writeState(3, config.id, "state.dat");
        writeState(3, config.id, "state1.dat");
        writeState(3, config.id, "state2.dat");
        writeState(1, config.id, "state3.dat");
        writeState(3, config.id, "state4.dat");
    }

    private void constructNodes() {
//        config.peers.clear();
//        config.peers.add(new InetSocketAddress(9004));
        leader = new Node(config);
        leader.addPreProcessInterceptors(new ForceLeader());


        ServerConfig followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9001)).withLogPath("log0.dat").withStatePath("state1.dat");
        List<InetSocketAddress> peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower0 = new Node(followerConfig.withPeers(peers));
        follower0.addPreProcessInterceptors(leaderStart);
        follower0.addPostProcessInterceptors(testEnd);

        followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9002)).withLogPath("log1.dat").withStatePath("state2.dat");
        peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower1 = new Node(followerConfig.withPeers(peers));
        follower1.addPreProcessInterceptors(leaderStart);
        follower1.addPostProcessInterceptors(testEnd);

        followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9003)).withLogPath("log2.dat").withStatePath("state3.dat");
        peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower2 = new Node(followerConfig.withPeers(peers));
        follower2.addPreProcessInterceptors(leaderStart);
        follower2.addPostProcessInterceptors(testEnd);

        followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9004)).withLogPath("log3.dat").withStatePath("state4.dat");
        peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower3 = new Node(followerConfig.withPeers(peers));
        follower3.addPreProcessInterceptors(leaderStart);
        follower3.addPostProcessInterceptors(testEnd);

        nodes.add(leader);
        nodes.add(follower0);
        nodes.add(follower1);
        nodes.add(follower2);
        nodes.add(follower3);
    }

    @After
    public void tearDown(){
        nodes.forEach(node -> {
           wrap(() -> node.stop().get());
           delete(node.config.logPath);
           delete(node.config.statePath);
        });

        nodes.clear();
    }

    @Test
    @SneakyThrows
    public void all_nodes_logs_should_be_in_sync_with_leaders_log(){
        assertNotEquals(leader.log, follower0.log);
        assertEquals(leader.log, follower1.log);
        assertNotEquals(leader.log, follower2.log);
        assertNotEquals(leader.log, follower3.log);

        leader.start();
        leaderStartLatch.countDown();

        follower0.start();
        follower1.start();
        follower2.start();
        follower3.start();

       testEndLatch.await();
       Thread.sleep(2000);  // wait a little bit for logs to flush to disk

        assertEquals("follower0's log not in sync with leader's", leader.log, follower0.log);
        assertEquals("follower0's log not in sync with leader's", leader.log, follower1.log);
        assertEquals("follower0's log not in sync with leader's", leader.log, follower2.log);
        assertEquals("follower0's log not in sync with leader's", leader.log, follower3.log);
    }
}
