package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.rpc.AppendEntriesReply;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.test.support.LogDomainSupport;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.josiahebhomenye.test.support.UserEventCapture;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Collections;

import static org.junit.Assert.*;

public class NodeTest implements StateDataSupport, LogDomainSupport {

    Node node;
    ServerConfig config;
    EmbeddedChannel channel;

    UserEventCapture userEventCapture;
    UserEventCapture captureDownstream;

    @Before
    @SneakyThrows
    public void setup(){
        initializeNode();
    }

    @After
    @SneakyThrows
    public void tearDown(){
        node.stop().get();
        delete(config.logPath);
        delete(config.statePath);
    }

    private void initializeNode(){
        userEventCapture = new UserEventCapture();
        captureDownstream = new UserEventCapture();
        config = new ServerConfig(ConfigFactory.load());
        node = new Node(config);
        channel = new EmbeddedChannel(userEventCapture, node, captureDownstream);
        node.channel = channel;
    }

    @Test
    public void ensureInitialStateOnFirstBoot(){
        assertEquals(0, node.currentTerm());
        assertNull(node.votedFor());
        assertEquals(NodeState.NULL_STATE(), node.state());
        assertTrue(node.log().isEmpty());
    }

    @Test
    @SneakyThrows
    public void ensuredStoredStateIsLoadedOnBoot(){
        writeState(1, new InetSocketAddress(8080), "state.dat");
        writeLog(Collections.singletonList(new LogEntry(1, new Set(5).serialize())), "log.dat");
        node.stop().get();
        initializeNode();

        assertEquals(1, node.currentTerm());
        assertEquals(NodeState.NULL_STATE(), node.state());
        assertEquals(new InetSocketAddress(8080), node.votedFor());
        assertFalse(node.log().isEmpty());
        assertEquals(new LogEntry(1, new Set(5).serialize()), node.log().get(1));

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

    @Test
    public void new_entry_should_be_added_to_end_of_log() throws Exception{
        writeState(leaderEntries().getLast().getTerm(), config.id, "state.dat");
        writeLog(leaderEntries(), "log.dat");
        node.stop().get();
        initializeNode();
        node.state.transitionTo(NodeState.LEADER());

        LogEntry newEntry = new LogEntry(node.currentTerm, new Add(5).serialize());
        ReceivedRequestEvent event = new ReceivedRequestEvent(new Request(newEntry.getCommand()), channel);
        channel.pipeline().fireUserEventTriggered(event);

        assertEquals(newEntry, node.log.lastEntry());
    }

    @Test
    public void do_not_process_any_more_events_after_shutdown() throws Exception{
        captureDownstream.ignore(StopEvent.class);

        Peer peer = new Peer(new InetSocketAddress("localhost", 8080), node, null);
        peer.set(channel);
        node.stop().get();

        channel.pipeline().fireUserEventTriggered(new AppendEntriesReplyEvent(new AppendEntriesReply(0, true), peer));
        channel.pipeline().fireUserEventTriggered(new AppendEntriesReplyEvent(new AppendEntriesReply(0, true), peer));
        channel.pipeline().fireUserEventTriggered(new AppendEntriesReplyEvent(new AppendEntriesReply(0, true), peer));

        assertEquals(0, captureDownstream.captured());
    }

}