package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.server.event.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.Instant;

import static org.junit.Assert.*;
import static com.josiahebhomenye.raft.server.core.NodeState.*;

public class CandidateTest extends NodeStateTest {

    Candidate candidate;

    @Before
    public void setup(){
        peers.forEach(peer -> node.handle(new PeerConnectedEvent(peer)));
    }

    @Override
    public NodeState initializeState() {
        candidate = NodeState.CANDIDATE;
        candidate.set(node);
        return candidate;
    }

    @Test
    public void start_election_on_initialization(){
        candidate.init();

        assertEquals(1L, node.currentTerm);
        assertEquals(node.id, node.votedFor);

        ScheduleTimeoutEvent event = userEventCapture.get(0);
        SendRequestVoteEvent requestVoteEvent = userEventCapture.get(1);

        assertTrue(event.timeout() >= 150 && event.timeout() <= 300);
        assertEquals(new RequestVote(1L, 0, 0, node.id), requestVoteEvent.requestVote());
    }

    @Test
    public void become_leader_if_received_majority_votes(){
        RequestVoteReply reply = new RequestVoteReply(1, true);
        candidate.handle(new RequestVoteReplyEvent(reply, channel));
        assertEquals(CANDIDATE, node.state);

        candidate.handle(new RequestVoteReplyEvent(reply, channel));
        assertEquals(CANDIDATE, node.state);

        candidate.handle(new RequestVoteReplyEvent(reply, channel));
        assertEquals(LEADER, node.state);

        StateTransitionEvent event = userEventCapture.get(0);
        assertEquals(new StateTransitionEvent(CANDIDATE, LEADER, node.id), event);
    }

    @Test
    public void become_leader_if_received_majority_votes_from_active_peers(){
        node.activePeers.clear();
        node.activePeers.put(new InetSocketAddress("localhost", 9000), new Peer(null, null, null));

        RequestVoteReply reply = new RequestVoteReply(1, true);
        candidate.handle(new RequestVoteReplyEvent(reply, channel));

        assertEquals(LEADER, node.state);

        StateTransitionEvent event = userEventCapture.get(0);
        assertEquals(new StateTransitionEvent(CANDIDATE, LEADER, node.id), event);
    }

    @Test
    public void stay_as_candidate_if_append_entries_term_is_less_then_current_term(){
        node.currentTerm = 2;
        long leaderTerm = 1;
        long leaderCommit = 0;
        long prevLogIndex = 0;
        long prevLogTerm = 1;

        AppendEntries appendEntries = AppendEntries.heartbeat(leaderTerm, prevLogIndex, prevLogTerm, leaderCommit, leaderId);
        AppendEntriesEvent expectedAppendEntriesEvent = new AppendEntriesEvent(appendEntries, channel);


        candidate.handle(expectedAppendEntriesEvent);

        assertEquals(0, userEventCapture.captured());
        assertEquals(CANDIDATE, node.state);

        AppendEntriesReply expected = new AppendEntriesReply(2, false);
        AppendEntriesReply actual = channel.readOutbound();

        assertEquals(expected, actual);
    }

    @Test
    public void start_new_election_on_election_timeout(){
        candidate.init();

        assertEquals(1L, node.currentTerm);
        assertEquals(node.id, node.votedFor);
        assertEquals(1, node.votes);
        userEventCapture.clear();

        candidate.handle(new ElectionTimeoutEvent(Instant.now(), node.id));

        assertEquals(2L, node.currentTerm);
        assertEquals(node.id, node.votedFor);
        assertEquals(1, node.votes);

        ScheduleTimeoutEvent event = userEventCapture.get(0);
        SendRequestVoteEvent requestVoteEvent = userEventCapture.get(1);

        assertTrue(event.timeout() >= 150 && event.timeout() <= 300);
        assertEquals(new RequestVote(2L, 0, 0, node.id), requestVoteEvent.requestVote());
    }
}
