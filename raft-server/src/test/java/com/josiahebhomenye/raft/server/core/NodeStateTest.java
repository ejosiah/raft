package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.server.config.ElectionTimeout;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.raft.server.support.StateDataSupport;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.josiahebhomenye.raft.server.core.NodeState.*;
import static org.junit.Assert.*;

public abstract class NodeStateTest implements StateDataSupport {

    EventLoopGroup group;
    UserEventCapture userEventCapture;
    EmbeddedChannel channel;
    NodeState state;
    Node node;
    InetSocketAddress leaderId;
    List<Peer> peers;
    ServerConfig config = new ServerConfig(ConfigFactory.load());

    @Before
    @SneakyThrows
    public void setup0(){
        try {
            Files.delete(Paths.get("log.dat"));
            Files.delete(Paths.get("state.dat"));
        } catch (NoSuchFileException e) {
            // ignore
        }

        group = new DefaultEventLoopGroup();
        userEventCapture = new UserEventCapture();
        ServerConfig config = new ServerConfig(ConfigFactory.load());
        node = new Node(config);
        node.group = group;
        channel = new EmbeddedChannel(userEventCapture);
        node.channel = channel;
        node.id = new InetSocketAddress("localhost", 8000);
        state = initializeState();
        leaderId = new InetSocketAddress("localhost", 9000);
        node.log.clear();
        userEventCapture.clear();
        node.activePeers.clear();

        peers = new ArrayList<>();


        peers.add(new Peer(new InetSocketAddress(9001), node, null).set(channel));
        peers.add(new Peer(new InetSocketAddress(9002), node, null).set(channel));
        peers.add(new Peer(new InetSocketAddress(9003), node, null).set(channel));
        peers.add(new Peer(new InetSocketAddress(9004), node, null).set(channel));
    }

    @After
    public void tearDown0(){
        node.stop();
        deleteState();
    }

    @Test
    public void revert_to_follower_if_append_entries_received_from_new_leader(){
        if(state.equals(FOLLOWER())) return;
        node.currentTerm = 1;
        long leaderTerm = 2;
        long leaderCommit = 3;
        long prevLogIndex = 3;
        long prevLogTerm = 2;

        AppendEntries appendEntries = AppendEntries.heartbeat(leaderTerm, prevLogIndex, prevLogTerm, leaderCommit, leaderId);
        AppendEntriesEvent expectedAppendEntriesEvent = new AppendEntriesEvent(appendEntries, channel);

        state.handle(expectedAppendEntriesEvent);

        StateTransitionEvent event = userEventCapture.get(StateTransitionEvent.class).get();
        AppendEntriesEvent appendEntriesEvent = userEventCapture.get(AppendEntriesEvent.class).get();

        assertEquals(new StateTransitionEvent(state, FOLLOWER(), node.id), event);
        assertEquals(expectedAppendEntriesEvent, appendEntriesEvent);
        assertEquals(FOLLOWER(), node.state);
    }

    @Test
    public void revert_to_follower_if_request_vote_received_with_higher_term(){
        if(state.equals(FOLLOWER())) return;
        node.currentTerm = 1;

        RequestVote requestVote = new RequestVote(2, 2, 2, leaderId);
        RequestVoteEvent expected = new RequestVoteEvent(requestVote, channel);

        state.handle(expected);

        StateTransitionEvent event = userEventCapture.get(StateTransitionEvent.class).get();
        RequestVoteEvent actual = userEventCapture.get(RequestVoteEvent.class).get();

        assertEquals(new StateTransitionEvent(state, FOLLOWER(), node.id), event);
        assertEquals(expected, actual);
        assertEquals(FOLLOWER(), node.state);
    }

    void assertElectionTimeout(ScheduleTimeoutEvent event){
        long minElectionTimeout = ((ElectionTimeout)config.electionTimeout).lower().toMillis();
        long maxElectionTimeout = ((ElectionTimeout)config.electionTimeout).upper().toMillis();

        assertTrue(event.timeout() >= minElectionTimeout && event.timeout() <= maxElectionTimeout);
    }

    public abstract NodeState initializeState();
}
