package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.PeerConnectedEvent;
import com.josiahebhomenye.raft.server.event.PeerDisconnectedEvent;
import com.josiahebhomenye.raft.server.event.ConnectPeerEvent;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.josiahebhomenye.test.support.UserEventCapture;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.function.Supplier;
import static org.junit.Assert.*;

public class NodeTest implements StateDataSupport {

    Node node;
    ServerConfig config;
    EmbeddedChannel channel;

    UserEventCapture userEventCapture;

    @Before
    @SneakyThrows
    public void setup(){
        try {
            Files.delete(Paths.get("state.dat"));
            Files.delete(Paths.get("log.dat"));
        } catch (NoSuchFileException e) {
            // ignore
        }
        initializeNode();
    }

    @After
    public void tearDown(){
        node.stop();
        deleteState();
    }

    private void initializeNode(){
        userEventCapture = new UserEventCapture();
        config = new ServerConfig(ConfigFactory.load());
        node = new Node(config);
        channel = new EmbeddedChannel(userEventCapture);
        node.channel = channel;
    }

    @Test
    public void ensureInitialStateOnFirstBoot(){
        assertEquals(0, node.getCurrentTerm());
        assertNull(node.getVotedFor());
        assertEquals(NodeState.NULL_STATE(), node.getState());
        assertTrue(node.getLog().isEmpty());
    }

    @Test
    @SneakyThrows
    public void ensuredStoredStateIsLoadedOnBoot(){
        Supplier<Void> createStateFile = () -> {
            try(DataOutputStream out = new DataOutputStream(new FileOutputStream("state.dat"))){
                out.writeLong(1);
                out.writeUTF("localhost");
                out.writeInt(8080);
            }catch (Exception e){

            }
            return null;
        };

        Supplier<Void> createLogFile = () -> {
            try(Log log = new Log("log.dat", 8)){
                log.add(new LogEntry(1, new Set(5).serialize()), 1);
            }catch (Exception e){

            }

            return null;
        };


        createStateFile.get();
        createLogFile.get();
        initializeNode();

        assertEquals(1, node.getCurrentTerm());
        assertEquals(NodeState.NULL_STATE(), node.getState());
        assertEquals(new InetSocketAddress("localhost", 8080), node.getVotedFor());
        assertFalse(node.getLog().isEmpty());
        assertEquals(new LogEntry(1, new Set(5).serialize()), node.getLog().get(1));

    }

    @Test
    public void peer_added_to_active_peers_list_on_connection(){
        assertTrue(node.activePeers.isEmpty());
        node.handle(new PeerConnectedEvent(new Peer(new InetSocketAddress("localhost", 8080), node, null)));

        assertEquals(1, node.activePeers.size());
    }

    @Test
    public void connection_should_be_retried_when_peer_gets_disconnected(){
        Peer peer = new Peer(new InetSocketAddress("localhost", 8080), node, null);
        peer.set(channel);

        assertTrue(node.activePeers.isEmpty());

        node.handle(new PeerConnectedEvent(peer));

        assertEquals(1, node.activePeers.size());

        node.handle(new PeerDisconnectedEvent(peer));

        assertTrue(node.activePeers.isEmpty());

        ConnectPeerEvent event = userEventCapture.get(0);
        assertEquals(new ConnectPeerEvent(peer), event);

    }

}