package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.raft.server.support.NodeWaitLatch;
import com.josiahebhomenye.raft.server.support.PreElectionSetup;
import com.josiahebhomenye.raft.server.support.RemotePeerMock;
import com.josiahebhomenye.raft.server.support.RequestRequestVoteForCount;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;
import static com.josiahebhomenye.raft.server.core.NodeState.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// FIXME move to integration folder and run independently of unit test
public class LeaderElectionTest {

    UserEventCapture userEventCapture = new UserEventCapture();
    List<RemotePeerMock> peers = new ArrayList<>();
    ServerConfig config = new ServerConfig(ConfigFactory.load());
    Node node;

    @Before
    public void setup(){

        userEventCapture.ignore(PeerConnectedEvent.class, AppendEntriesEvent.class, RequestVoteReplyEvent.class);

        config.peers.forEach(address -> peers.add(new RemotePeerMock(address, config.id)));
        peers.forEach(RemotePeerMock::start);
    }

    @After
    public void tearDown(){
        peers.forEach(RemotePeerMock::stop);
        node.stop();
        node = null;
    }


    @Test
    @SneakyThrows
    public void become_leader_when_majority_vote_received(){
        peers.forEach(peer -> {
            peer.whenRequestVote((ctx, msg) -> {
                ctx.writeAndFlush(new RequestVoteReply(msg.getTerm(), true));
            });
        });
        CountDownLatch latch = new CountDownLatch(1);

        node = new Node(config);
        node.addPreProcessInterceptors(userEventCapture);
        node.addPostProcessInterceptors(new NodeWaitLatch(latch, (Node, evt) -> {
           return evt instanceof HeartbeatTimeoutEvent && node.currentTerm == 1;
        }));
        node.start();

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(1L, node.currentTerm);
        assertEquals(node.id, node.votedFor);
        assertEquals(LEADER, node.state);

        assertEquals(new StateTransitionEvent(NULL_STATE, FOLLOWER, node.id), userEventCapture.get(0));
        assertTrue(userEventCapture.get(1) instanceof ScheduleTimeoutEvent);
        assertTrue(userEventCapture.get(2) instanceof ElectionTimeoutEvent);
        assertEquals(new StateTransitionEvent(FOLLOWER, CANDIDATE, node.id), userEventCapture.get(3));
        assertTrue(userEventCapture.get(4) instanceof ScheduleTimeoutEvent);
        assertEquals(new SendRequestVoteEvent(new RequestVote(1L, 0, 0, node.id)), userEventCapture.get(5));
        assertEquals(new StateTransitionEvent(CANDIDATE, LEADER, node.id), userEventCapture.get(6));
        assertEquals(new ScheduleHeartbeatTimeoutEvent(node.id, 50L), userEventCapture.get(7));
        assertEquals(new HeartbeatTimeoutEvent(node.id), userEventCapture.get(8));
    }

    @Test
    @SneakyThrows
    public void restart_election_on_split_votes(){
        peers.stream().limit(3).forEach(peer -> {
            peer.whenRequestVote(new RequestRequestVoteForCount(1));
        });

        peers.stream().skip(3).forEach(peer -> {
            peer.whenRequestVote((ctx, msg) -> {
                ctx.writeAndFlush(new RequestVoteReply(msg.getTerm(), true));
            });
        });

        CountDownLatch latch = new CountDownLatch(1);

        node = new Node(config);
        node.addPreProcessInterceptors(userEventCapture);
        node.addPostProcessInterceptors(new NodeWaitLatch(latch, (Node, evt) -> {
            return evt instanceof HeartbeatTimeoutEvent && node.currentTerm == 2;
        }));
        node.start();

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(2L, node.currentTerm);
        assertEquals(node.id, node.votedFor);
        assertEquals(LEADER, node.state);

        assertEquals(new StateTransitionEvent(NULL_STATE, FOLLOWER, node.id), userEventCapture.get(0));
        assertTrue(userEventCapture.get(1) instanceof ScheduleTimeoutEvent);
        assertTrue(userEventCapture.get(2) instanceof ElectionTimeoutEvent);
        assertEquals(new StateTransitionEvent(FOLLOWER, CANDIDATE, node.id), userEventCapture.get(3));

        assertTrue(userEventCapture.get(4) instanceof ScheduleTimeoutEvent);
        assertEquals(new SendRequestVoteEvent(new RequestVote(1L, 0, 0, node.id)), userEventCapture.get(5));

        // Election Timeout due to split votes
        assertTrue(userEventCapture.get(6) instanceof ElectionTimeoutEvent);
        assertTrue(userEventCapture.get(7) instanceof ScheduleTimeoutEvent);

        // star new election for term 2
        assertEquals(new SendRequestVoteEvent(new RequestVote(2L, 0, 0, node.id)), userEventCapture.get(8));
        assertEquals(new StateTransitionEvent(CANDIDATE, LEADER, node.id), userEventCapture.get(9));
        assertEquals(new ScheduleHeartbeatTimeoutEvent(node.id, 50L), userEventCapture.get(10));
        assertEquals(new HeartbeatTimeoutEvent(node.id), userEventCapture.get(11));
    }

    @Test
    @SneakyThrows
    public void revert_to_follower_if_another_node_elected_leader(){

        peers.stream().limit(1).forEach(peer -> {
            peer.whenRequestVote((ctx, msg) -> {
                peer.send(AppendEntries.heartbeat(1, 0, 0, 0, peer.address), 50L);
                peer.reset();
            });
        });

        CountDownLatch latch = new CountDownLatch(1);

        node = new Node(config);
        node.addPreProcessInterceptors(new PreElectionSetup(peers));
        node.addPreProcessInterceptors(new NodeWaitLatch(latch, (Node, evt) -> {
            return evt.equals(new StateTransitionEvent(CANDIDATE, FOLLOWER, node.id));
        }));
        node.addPreProcessInterceptors(userEventCapture);

        node.start();

        if(!latch.await(10, TimeUnit.SECONDS)){
            LoggerFactory.getLogger(this.getClass()).info("latch did not release before timeout");
        }
        node.stop();

        assertEquals(1L, node.currentTerm);
        assertEquals(node.id, node.votedFor);
        assertEquals(FOLLOWER, node.state);

        assertEquals(new StateTransitionEvent(NULL_STATE, FOLLOWER, node.id), userEventCapture.get(0));
        assertTrue(userEventCapture.get(1) instanceof ScheduleTimeoutEvent);
        assertTrue(userEventCapture.get(2) instanceof ElectionTimeoutEvent);
        assertEquals(new StateTransitionEvent(FOLLOWER, CANDIDATE, node.id), userEventCapture.get(3));
        assertTrue(userEventCapture.get(4) instanceof ScheduleTimeoutEvent);
        assertEquals(new SendRequestVoteEvent(new RequestVote(1L, 0, 0, node.id)), userEventCapture.get(5));
        assertEquals(new StateTransitionEvent(CANDIDATE, FOLLOWER, node.id), userEventCapture.get(6));
    }
}
