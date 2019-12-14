package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Data;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.support.ForceLeader;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogReplicationTest {

    public ServerConfig config = new ServerConfig(ConfigFactory.load());
    Node leader;
    Node follower0;
    Node follower1;
    Node follower2;
    Node follower3;

    @Before
    public void setup(){
        buildLogEntries();
        constructStateData();
        constructNodes();

    }

    void buildLogEntries(){
        Log leaderLog = new Log("log.dat");
        Log follower0Log = new Log("log0.dat");
        Log follower1Log = new Log("log1.dat");
        Log follower2Log = new Log("log2.dat");
        Log follower3Log = new Log("log3.dat");

        leaderLog.add(new LogEntry(1, new Set(3)), 1);
        leaderLog.add(new LogEntry(1, new Set(1)), 2);
        leaderLog.add(new LogEntry(1, new Set(9)), 3);
        leaderLog.add(new LogEntry(2, new Set(2)), 4);
        leaderLog.add(new LogEntry(3, new Set(0)), 5);
        leaderLog.add(new LogEntry(3, new Set(7)), 6);
        leaderLog.add(new LogEntry(3, new Set(5)), 7);
        leaderLog.add(new LogEntry(3, new Set(4)), 8);

        follower0Log.add(new LogEntry(1, new Set(3)), 1);
        follower0Log.add(new LogEntry(1, new Set(1)), 2);
        follower0Log.add(new LogEntry(1, new Set(9)), 3);
        follower0Log.add(new LogEntry(2, new Set(2)), 4);
        follower0Log.add(new LogEntry(3, new Set(0)), 5);


        follower1Log.add(new LogEntry(1, new Set(3)), 1);
        follower1Log.add(new LogEntry(1, new Set(1)), 2);
        follower1Log.add(new LogEntry(1, new Set(9)), 3);
        follower1Log.add(new LogEntry(2, new Set(2)), 4);
        follower1Log.add(new LogEntry(3, new Set(0)), 5);
        follower1Log.add(new LogEntry(3, new Set(7)), 6);
        follower1Log.add(new LogEntry(3, new Set(5)), 7);
        follower1Log.add(new LogEntry(3, new Set(4)), 8);

        follower2Log.add(new LogEntry(1, new Set(3)), 1);
        follower2Log.add(new LogEntry(1, new Set(1)), 2);

        follower3Log.add(new LogEntry(1, new Set(3)), 1);
        follower3Log.add(new LogEntry(1, new Set(1)), 2);
        follower3Log.add(new LogEntry(1, new Set(9)), 3);
        follower3Log.add(new LogEntry(2, new Set(2)), 4);
        follower3Log.add(new LogEntry(3, new Set(0)), 5);
        follower3Log.add(new LogEntry(3, new Set(7)), 6);
        follower3Log.add(new LogEntry(3, new Set(5)), 7);

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

    void writeState(long term, InetSocketAddress votedFor, String path){
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream(path))){
            out.writeLong(term);
            out.writeUTF(votedFor.getHostName());
            out.writeInt(votedFor.getPort());
        }catch(Exception ex){

        }
    }

    private void constructNodes() {
        leader = new Node(config);
        leader.addPreProcessInterceptors(new ForceLeader());


        ServerConfig followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9001)).withLogPath("log0.dat").withStatePath("state1.dat");
        List<InetSocketAddress> peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower0 = new Node(followerConfig.withPeers(peers));

        followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9002)).withLogPath("log1.dat").withStatePath("state2.dat");
        peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower1 = new Node(followerConfig.withPeers(peers));

        followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9003)).withLogPath("log2.dat").withStatePath("state3.dat");
        peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower2 = new Node(followerConfig.withPeers(peers));

        followerConfig = new ServerConfig(ConfigFactory.load()).withId(new InetSocketAddress(9004)).withLogPath("log3.dat").withStatePath("state4.dat");
        peers = followerConfig.peers;
        peers.remove(followerConfig.id);
        peers.add(leader.id);
        follower3 = new Node(followerConfig.withPeers(peers));
    }

    @After
    public void tearDown(){
        leader.stop();
        follower0.stop();
        follower1.stop();
        follower2.stop();
        follower3.stop();
    }

    @Test
    @SneakyThrows
    public void all_nodes_logs_should_be_in_sync_with_leaders_log(){
        assertNotEquals(leader.log, follower0.log);
        assertEquals(leader.log, follower1.log);
        assertNotEquals(leader.log, follower2.log);
        assertNotEquals(leader.log, follower3.log);

        leader.start();
        Thread.sleep(1000);

        follower0.start();
//        follower1.start();
//        follower2.start();
//        follower3.start();

        Thread.sleep(1000);

        assertEquals(leader.log, follower0.log);
//        assertEquals(leader.log, follower1.log);
//        assertEquals(leader.log, follower2.log);
//        assertEquals(leader.log, follower3.log);
    }

}
