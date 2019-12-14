package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.comand.Subtract;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteEvent;
import com.josiahebhomenye.raft.server.event.StateTransitionEvent;
import com.typesafe.config.ConfigFactory;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import lombok.SneakyThrows;
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

public abstract class NodeStateTest {

    EventLoopGroup group;
    UserEventCapture userEventCapture;
    EmbeddedChannel channel;
    NodeState state;
    Node node;
    InetSocketAddress leaderId;
    List<Peer> peers;

    @Before
    @SneakyThrows
    public void setup0(){
        try {
           // Files.delete(Paths.get("log.dat"));
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


        peers.add(new Peer(new InetSocketAddress(9001), node.channel, null).set(channel));
        peers.add(new Peer(new InetSocketAddress(9002), node.channel, null).set(channel));
        peers.add(new Peer(new InetSocketAddress(9003), node.channel, null).set(channel));
        peers.add(new Peer(new InetSocketAddress(9004), node.channel, null).set(channel));
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

        StateTransitionEvent event = userEventCapture.get(0);
        AppendEntriesEvent appendEntriesEvent = userEventCapture.get(1);

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

        StateTransitionEvent event = userEventCapture.get(0);
        RequestVoteEvent actual = userEventCapture.get(1);

        assertEquals(new StateTransitionEvent(state, FOLLOWER(), node.id), event);
        assertEquals(expected, actual);
        assertEquals(FOLLOWER(), node.state);
    }

    public abstract NodeState initializeState();
}
