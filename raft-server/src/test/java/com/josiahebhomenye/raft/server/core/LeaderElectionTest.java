package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.raft.server.support.RemotePeerMock;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static com.josiahebhomenye.raft.server.core.NodeState.*;
import java.util.ArrayList;
import java.util.List;

public class LeaderElectionTest {

    UserEventCapture userEventCapture = new UserEventCapture();
    List<RemotePeerMock> peers = new ArrayList<>();
    ServerConfig config = new ServerConfig(ConfigFactory.load());

    @Before
    public void setup(){

        userEventCapture.ignore(PeerConnectedEvent.class, AppendEntriesEvent.class, RequestVoteReplyEvent.class);

        config.peers.forEach(address -> peers.add(new RemotePeerMock(address)));
        peers.forEach(RemotePeerMock::start);
    }

    @After
    public void tearDown(){
        peers.forEach(RemotePeerMock::stop);
    }


    @Test
    @SneakyThrows
    public void become_leader_when_majority_vote_received(){
        peers.forEach(peer -> {
            peer.whenRequestVote((ctx, msg) -> {
                ctx.writeAndFlush(new RequestVoteReply(msg.getTerm(), true));
            });
        });

        Node node = new Node(config);
        node.addPreProcessInterceptors(userEventCapture);
        node.start();

        Thread.sleep(1500);

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
}
