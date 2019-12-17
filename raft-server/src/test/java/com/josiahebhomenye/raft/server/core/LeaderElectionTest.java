package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.rpc.AppendEntries;
import com.josiahebhomenye.raft.rpc.RequestVote;
import com.josiahebhomenye.raft.rpc.RequestVoteReply;
import com.josiahebhomenye.raft.server.config.ElectionTimeout;
import com.josiahebhomenye.raft.server.config.HeartbeatTimeout;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.raft.server.support.*;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.josiahebhomenye.test.support.UserEventCapture;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static com.josiahebhomenye.raft.server.core.NodeState.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

// FIXME move to integration folder and run independently of unit test
public class LeaderElectionTest implements StateDataSupport {

    UserEventCapture userEventCapture = new UserEventCapture();
    List<RemotePeerMock> peers = new ArrayList<>();
    ServerConfig config = new ServerConfig(ConfigFactory.load());
    CountDownLatch testLatch;
    CountDownLatch nodeLatch;
    Node node;

    ElectionTimeout electionTimeout = new ElectionTimeout(Duration.of(150, ChronoUnit.MILLIS), Duration.of(300, ChronoUnit.MILLIS));
    HeartbeatTimeout heartbeatTimeout = new HeartbeatTimeout(Duration.of(50, ChronoUnit.MILLIS));

    @Before
    public void setup(){
        delete(config.logPath);
        delete(config.statePath);
        config = config.withElectionTimeout(electionTimeout).withHeartbeatTimeout(heartbeatTimeout);

        testLatch = new CountDownLatch(1);
        nodeLatch = new CountDownLatch(1);
        userEventCapture.ignore(PeerConnectedEvent.class, AppendEntriesEvent.class, RequestVoteReplyEvent.class);

        config.peers.forEach(address -> peers.add(new RemotePeerMock(address, config.id)));
        peers.forEach(RemotePeerMock::start);
    }

    @After
    @SneakyThrows
    public void tearDown(){
        peers.forEach(RemotePeerMock::stop);
        nodeLatch.countDown();
        node.stop().get();
        delete(config.logPath);
        delete(config.statePath);
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

        node = new Node(config);
        node.addPreProcessInterceptors(userEventCapture);
        node.addPostProcessInterceptors(new NodeWaitLatch(testLatch, nodeLatch, (Node, evt) -> {
           return evt instanceof ScheduleHeartbeatTimeoutEvent && node.currentTerm == 1;
        }));
        node.start();

        testLatch.await(10, TimeUnit.SECONDS);

        assertEquals(1L, node.currentTerm);
        assertEquals(node.id, node.votedFor);
        assertEquals(LEADER(), node.state);

        assertEquals("expected a StateTransitionEvent to be triggered", new StateTransitionEvent(NULL_STATE(), FOLLOWER(), node.id), userEventCapture.get(0));
        assertTrue("expected a ScheduleTimeoutEvent to be triggered", userEventCapture.get(1) instanceof ScheduleTimeoutEvent);
        assertTrue("expected a ElectionTimeoutEvent to be triggered", userEventCapture.get(2) instanceof ElectionTimeoutEvent);
        assertEquals("expected a StateTransitionEvent to be triggered", new StateTransitionEvent(FOLLOWER(), CANDIDATE(), node.id), userEventCapture.get(3));
        assertTrue("expected a ScheduleTimeoutEvent to be triggered", userEventCapture.get(4) instanceof ScheduleTimeoutEvent);
        assertEquals("expected a SendRequestVoteEvent to be triggered", new SendRequestVoteEvent(new RequestVote(1L, 0, 0, node.id)), userEventCapture.get(5));
        assertEquals("expected a StateTransitionEvent to be triggered", new StateTransitionEvent(CANDIDATE(), LEADER(), node.id), userEventCapture.get(6));
        assertEquals("expected a ScheduleHeartbeatTimeoutEvent to be triggered", new ScheduleHeartbeatTimeoutEvent(node.id, config.heartbeatTimeout.get()), userEventCapture.get(7));
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

        node = new Node(config);
        node.addPreProcessInterceptors(userEventCapture);
        node.addPostProcessInterceptors(new NodeWaitLatch(testLatch, nodeLatch, (Node, evt) -> {
            return evt instanceof ScheduleHeartbeatTimeoutEvent && node.currentTerm == 2;
        }));

        node.start();

        testLatch.await(10, TimeUnit.SECONDS);

        assertEquals(2L, node.currentTerm);
        assertEquals(node.id, node.votedFor);
        assertEquals(LEADER(), node.state);

        assertEquals(new StateTransitionEvent(NULL_STATE(), FOLLOWER(), node.id), userEventCapture.get(0));
        assertTrue(userEventCapture.get(1) instanceof ScheduleTimeoutEvent);
        assertTrue(userEventCapture.get(2) instanceof ElectionTimeoutEvent);
        assertEquals(new StateTransitionEvent(FOLLOWER(), CANDIDATE(), node.id), userEventCapture.get(3));

        assertTrue(userEventCapture.get(4) instanceof ScheduleTimeoutEvent);
        assertEquals(new SendRequestVoteEvent(new RequestVote(1L, 0, 0, node.id)), userEventCapture.get(5));

        // Election Timeout due to split votes
        assertTrue(userEventCapture.get(6) instanceof ElectionTimeoutEvent);
        assertTrue(userEventCapture.get(7) instanceof ScheduleTimeoutEvent);

        // star new election for term 2
        assertEquals(new SendRequestVoteEvent(new RequestVote(2L, 0, 0, node.id)), userEventCapture.get(8));
        assertEquals(new StateTransitionEvent(CANDIDATE(), LEADER(), node.id), userEventCapture.get(9));
        assertEquals(new ScheduleHeartbeatTimeoutEvent(node.id, config.heartbeatTimeout.get()), userEventCapture.get(10));
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

        node = new Node(config);
        node.addPreProcessInterceptors(new PreElectionSetup(peers));
        node.addPreProcessInterceptors(userEventCapture);
        node.addPostProcessInterceptors(new NodeWaitLatch(testLatch, nodeLatch, (Node, evt) -> {
            return evt.equals(new StateTransitionEvent(CANDIDATE(), FOLLOWER(), node.id));
        }));

        node.start();

        testLatch.await(10, TimeUnit.SECONDS);

        assertEquals(1L, node.currentTerm);
        assertEquals(FOLLOWER(), node.state);

        assertEquals(new StateTransitionEvent(NULL_STATE(), FOLLOWER(), node.id), userEventCapture.get(0));
        assertTrue(userEventCapture.get(1) instanceof ScheduleTimeoutEvent);
        assertTrue(userEventCapture.get(2) instanceof ElectionTimeoutEvent);
        assertEquals(new StateTransitionEvent(FOLLOWER(), CANDIDATE(), node.id), userEventCapture.get(3));
        assertTrue(userEventCapture.get(4) instanceof ScheduleTimeoutEvent);
        assertEquals(new SendRequestVoteEvent(new RequestVote(1L, 0, 0, node.id)), userEventCapture.get(5));
        assertEquals(new StateTransitionEvent(CANDIDATE(), FOLLOWER(), node.id), userEventCapture.get(6));
    }

    @Test
    @SneakyThrows
    public void grant_vote_received_request_vote_from_another_candidate_with_higher_term(){
        RemotePeerMock mockPeer = peers.get(0);
        mockPeer.whenRequestVote((ctx, msg) ->  mockPeer.send(msg.withTerm(msg.getTerm()+1).withCandidateId(mockPeer.address)) );
        mockPeer.whenRequestVoteReply((ctx, msg) -> testLatch.countDown());

        node = new Node(config);
        node.addPreProcessInterceptors(new PreElectionSetup(peers));
        node.addPreProcessInterceptors(userEventCapture);

        RequestVoteReply expectedMsg = new RequestVoteReply(2, true);

//        node.addPostProcessInterceptors(new NodeWaitLatch(testLatch, nodeLatch, (Node, obj) -> {
//            return expectedMsg.equals(obj);
//        }));  TODO add interceptors to peers

        node.start();

        testLatch.await(10, TimeUnit.SECONDS);

        assertEquals(2L, node.currentTerm);
        assertEquals(FOLLOWER(), node.state);

        mockPeer.verifyMessageReceived(expectedMsg);
    }
}
