package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.rpc.RequestVote;
import com.josiahebhomenye.raft.server.config.ElectionTimeout;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.*;

import com.josiahebhomenye.test.support.StateDataSupport;
import com.josiahebhomenye.test.support.UserEventCapture;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.josiahebhomenye.raft.server.core.NodeState.*;
import static org.junit.Assert.*;

public abstract class NodeStateTest implements StateDataSupport {

    EventLoopGroup group;
    UserEventCapture userEventCapture;
    EmbeddedChannel nodeChannel;
    EmbeddedChannel peerChannel;
    EmbeddedChannel clientChannel;
    NodeState state;
    Node node;
    InetSocketAddress leaderId;
    LinkedList<Peer> peers;
    ServerConfig config = new ServerConfig(ConfigFactory.load());

    @Before
    @SneakyThrows
    public void setup0(){
        delete(config.logPath);
        delete(config.statePath);
        group = new DefaultEventLoopGroup();
        userEventCapture = new UserEventCapture();
        ServerConfig config = new ServerConfig(ConfigFactory.load());
        node = new Node(config);
        node.group = group;
        nodeChannel = new EmbeddedChannel(userEventCapture, node);
        node.channel = nodeChannel;
        node.id = new InetSocketAddress("localhost", 8000);
        state = initializeState();
        leaderId = new InetSocketAddress("localhost", 9000);
        node.log.clear();
        userEventCapture.clear();
        node.activePeers.clear();

        peers = new LinkedList<>();


        peers.add(new Peer(new InetSocketAddress(9001), node, null));
        peers.add(new Peer(new InetSocketAddress(9002), node, null));
        peers.add(new Peer(new InetSocketAddress(9003), node, null));
        peers.add(new Peer(new InetSocketAddress(9004), node, null));

        peerChannel = new EmbeddedChannel(
            userEventCapture,
            peers.get(0).connectionHandler,
            peers.get(1).connectionHandler,
            peers.get(2).connectionHandler,
            peers.get(3).connectionHandler
        );

        peers.forEach(p -> p.set(peerChannel));

        clientChannel = new EmbeddedChannel(userEventCapture);
        node.sender = clientChannel;
    }

    @After
    @SneakyThrows
    public void tearDown0(){
        node.stop().get();
        delete(config.logPath);
        delete(config.statePath);
    }

    @Test
    public void revert_to_follower_if_append_entries_received_from_new_leader(){
        if(state.equals(FOLLOWER)) return;
        node.currentTerm = 1;
        long leaderTerm = 2;
        long leaderCommit = 3;
        long prevLogIndex = 3;
        long prevLogTerm = 2;

        AppendEntries appendEntries = AppendEntries.heartbeat(leaderTerm, prevLogIndex, prevLogTerm, leaderCommit, leaderId);
        AppendEntriesEvent expectedAppendEntriesEvent = new AppendEntriesEvent(appendEntries, nodeChannel);

        state.handle(expectedAppendEntriesEvent);

        StateTransitionEvent event = userEventCapture.get(StateTransitionEvent.class).get();
        AppendEntriesEvent appendEntriesEvent = userEventCapture.get(AppendEntriesEvent.class).get();

        assertEquals(new StateTransitionEvent(state, FOLLOWER, node.channel), event);
        assertEquals(expectedAppendEntriesEvent, appendEntriesEvent);
        assertEquals(FOLLOWER, node.state);
    }

    @Test
    public void revert_to_follower_if_request_vote_received_with_higher_term(){
        if(state.equals(FOLLOWER)) return;
        node.currentTerm = 1;

        RequestVote requestVote = new RequestVote(2, 2, 2, leaderId);
        RequestVoteEvent expected = new RequestVoteEvent(requestVote, nodeChannel);

        state.handle(expected);

        StateTransitionEvent event = userEventCapture.get(StateTransitionEvent.class).get();
        RequestVoteEvent actual = userEventCapture.get(RequestVoteEvent.class).get();

        assertEquals(new StateTransitionEvent(state, FOLLOWER, node.channel), event);
        assertEquals(expected, actual);
        assertEquals(FOLLOWER, node.state);
    }

    void assertElectionTimeout(ScheduleTimeoutEvent event){
        long minElectionTimeout = ((ElectionTimeout)config.electionTimeout).lower().toMillis();
        long maxElectionTimeout = ((ElectionTimeout)config.electionTimeout).upper().toMillis();

        assertTrue(event.timeout() >= minElectionTimeout && event.timeout() <= maxElectionTimeout);
    }

    public abstract NodeState initializeState();
}
