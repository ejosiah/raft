package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.comand.*;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.rpc.*;
import com.josiahebhomenye.raft.server.event.*;
import lombok.SneakyThrows;
import org.junit.Test;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class FollowerTest extends NodeStateTest{

    Follower follower;

    @Override
    public NodeState initializeState() {
        follower = NodeState.FOLLOWER();
        follower.set(node);
        return follower;
    }

    @Test
    public void reply_false_to_append_entry_message_if_leaders_term_is_behind(){
        node.currentTerm = 2;
        AppendEntries entries = AppendEntries.heartbeat(node.currentTerm - 1, 0, 0, 0, leaderId);

        follower.handle(new AppendEntriesEvent(entries, nodeChannel));

        AppendEntriesReply expected = new AppendEntriesReply(2, false);
        AppendEntriesReply actual = nodeChannel.readOutbound();

        assertEquals(expected, actual);

    }

    @Test
    public void reply_false_to_append_entry_message_if_log_does_not_contain_an_entry_with_previous_log_term(){
        node.currentTerm = 4;

        node.log.add(new LogEntry(1, new Set(5).serialize()), 1);
        node.log.add(new LogEntry(2, new Add(1).serialize()), 2);
        node.log.add(new LogEntry(2, new Set(1).serialize()), 3);
        node.log.add(new LogEntry(3, new Set(2).serialize()), 4);

        AppendEntries entries = AppendEntries.heartbeat(node.currentTerm, 3, 3, 0, leaderId);

        follower.handle(new AppendEntriesEvent(entries, nodeChannel));

        AppendEntriesReply expected = new AppendEntriesReply(node.currentTerm, false);
        AppendEntriesReply actual = nodeChannel.readOutbound();

        assertEquals(expected, actual);
    }

    @Test
    public void delete_exiting_entries_that_conflict_with_new_entries(){
        node.currentTerm = 2;
        long leaderTerm = 3;
        long leaderCommit = 3;
        long prevLogIndex = 3;
        long prevLogTerm = 2;

        LogEntry conflictingEntry = new LogEntry(2, new Set(2).serialize());

        node.log.add(new LogEntry(1, new Set(5).serialize()), 1);
        node.log.add(new LogEntry(2, new Add(1).serialize()), 2);
        node.log.add(new LogEntry(2, new Set(1).serialize()), 3);
        node.log.add(conflictingEntry, 4);
        node.log.add(new LogEntry(2, new Add(1).serialize()), 5);

        List<byte[]> entries = new ArrayList<>();
        entries.add(new LogEntry(3, new Add(3).serialize()).serialize());

        AppendEntries appendEntries = new AppendEntries(leaderTerm, prevLogIndex, prevLogTerm, leaderCommit, leaderId, entries);

        follower.handle(new AppendEntriesEvent(appendEntries, nodeChannel));

        assertEquals(4, node.log.size());
        assertNotEquals(conflictingEntry, node.log.get(4)); // conflicting entry removed
        assertEquals(new LogEntry(3, new Add(3).serialize()), node.log.lastEntry()); // and replaced with entry from leader

        CommitEvent expectedCommitEvent = new CommitEvent(leaderCommit, node.id);

        assertEquals(expectedCommitEvent, userEventCapture.get(1));

        AppendEntriesReply expected = new AppendEntriesReply(leaderTerm, true);
        AppendEntriesReply actual = nodeChannel.readOutbound();

        assertEquals(expected, actual);
    }

    @Test
    public void append_new_entries_not_already_in_the_log(){
        node.currentTerm = 2;
        long leaderTerm = 3;
        long leaderCommit = 5;
        long prevLogIndex = 4;
        long prevLogTerm = 2;


        node.log.add(new LogEntry(1, new Set(5).serialize()), 1);
        node.log.add(new LogEntry(2, new Add(1).serialize()), 2);
        node.log.add(new LogEntry(2, new Set(1).serialize()), 3);
        node.log.add(new LogEntry(2, new Add(1).serialize()), 4);

        List<byte[]> entries = new ArrayList<>();
        entries.add(new LogEntry(3, new Add(3).serialize()).serialize());
        entries.add(new LogEntry(3, new Add(1).serialize()).serialize());
        entries.add(new LogEntry(3, new Add(4).serialize()).serialize());

        AppendEntries appendEntries = new AppendEntries(leaderTerm, prevLogIndex, prevLogTerm, leaderCommit, leaderId, entries);

        follower.handle(new AppendEntriesEvent(appendEntries, nodeChannel));

        assertEquals(7, node.log.size());
        assertEquals(new LogEntry(3, new Add(4).serialize()), node.log.lastEntry());

        CommitEvent expectedCommitEvent = new CommitEvent(leaderCommit, node.id);

        assertEquals(expectedCommitEvent, userEventCapture.get(CommitEvent.class).get());

        AppendEntriesReply expected = new AppendEntriesReply(leaderTerm, true);
        AppendEntriesReply actual = nodeChannel.readOutbound();

        assertEquals(expected, actual);
    }

    @Test
    public void initialization_should_schedule_election_timeout() throws Exception{
        follower.init();

        ScheduleTimeoutEvent event = userEventCapture.get(0);

        assertElectionTimeout(event);
        assertEquals(node.state, follower);
    }

    @Test
    public void follower_should_transition_to_candidate_when_no_heartbeat_received_before_election_timeout(){
        Instant lastHeartbeat = Instant.now().minus(Duration.ofMinutes(5));
        node.lastHeartbeat = lastHeartbeat;
        follower.handle(new ElectionTimeoutEvent(lastHeartbeat, node.id));

        assertEquals(node.state, NodeState.CANDIDATE());
    }

    @Test
    public void follower_should_transition_to_candidate_on_last_heartbeat_is_not_set(){
        node.lastHeartbeat = null;
        follower.handle(new ElectionTimeoutEvent(null, node.id));

        assertEquals(node.state, NodeState.CANDIDATE());
    }

    @Test
    @SneakyThrows
    public void follower_should_schedule_a_new_election_time_when_heartbeat_received_after_election_timeout(){
        node.lastHeartbeat = Instant.now();
        Instant lastHeartbeat = node.lastHeartbeat.minusMillis(1000);

        follower.handle(new ElectionTimeoutEvent(lastHeartbeat, node.id));

        ScheduleTimeoutEvent event = userEventCapture.get(0);

        assertElectionTimeout(event);
        assertEquals(node.state, follower);
    }

    @Test
    @SneakyThrows
    public void Follower_should_not_grant_vote_when_requestors_term_is_less_than_current_term(){
        node.currentTerm = 2;

        follower.handle(new PeerConnectedEvent(peers.getFirst()));

        RequestVote req = new RequestVote(1, 3, 1, peers.getFirst().id);
        RequestVoteEvent event = new RequestVoteEvent(req, peerChannel);

        follower.handle(event);

        RequestVoteReply reply = peerChannel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
        assertEquals(node.state, NodeState.FOLLOWER());
    }

    @Test
    @SneakyThrows
    public void follower_should_not_grant_vote_when_already_voted_for_another_candidate(){
        node.currentTerm = 2;
        node.votedFor = peers.get(1).id;

        follower.handle(new PeerConnectedEvent(peers.getFirst()));

        RequestVote req = new RequestVote(3, 3, 1, peers.getFirst().id);
        RequestVoteEvent event = new RequestVoteEvent(req, peerChannel);

        follower.handle(event);

        RequestVoteReply reply = peerChannel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
        assertEquals(node.state, NodeState.FOLLOWER());
    }

    @Test
    @SneakyThrows
    public void follower_should_not_grant_vote_when_candidates_last_log_term_entry_is_not_up_to_date(){
        node.currentTerm = 3;

        follower.handle(new PeerConnectedEvent(peers.getFirst()));

        node.log.add(new LogEntry(1, new Set(5).serialize()), 1);
        node.log.add(new LogEntry(2, new Add(2).serialize()), 2);
        node.log.add(new LogEntry(3, new Multiply(3).serialize()), 3);
        node.log.add(new LogEntry(3, new Subtract(1).serialize()), 4);

        RequestVote req = new RequestVote(3, 3, 1, peers.getFirst().id);
        RequestVoteEvent event = new RequestVoteEvent(req, peerChannel);

        follower.handle(event);

        RequestVoteReply reply = peerChannel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
        assertEquals(node.state, NodeState.FOLLOWER());
    }

    @Test
    @SneakyThrows
    public void follower_should_not_grant_vote_when_candidates_last_log_index_is_not_up_to_date(){
        node.currentTerm = 3;

        follower.handle(new PeerConnectedEvent(peers.getFirst()));

        node.log.add(new LogEntry(1, new Set(5).serialize()), 1);
        node.log.add(new LogEntry(2, new Add(2).serialize()), 2);
        node.log.add(new LogEntry(3, new Multiply(3).serialize()), 3);
        node.log.add(new LogEntry(3, new Subtract(1).serialize()), 4);

        RequestVote req = new RequestVote(3, 3, 3, peers.getFirst().id);
        RequestVoteEvent event = new RequestVoteEvent(req, peerChannel);

        follower.handle(event);

        RequestVoteReply reply = peerChannel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
        assertEquals(node.state, NodeState.FOLLOWER());
    }

    @Test
    @SneakyThrows
    public void grant_vote_when_all_conditions_are_met(){
        node.currentTerm = 2;

        follower.handle(new PeerConnectedEvent(peers.getFirst()));

        node.log.add(new LogEntry(1, new Set(5).serialize()), 1);
        node.log.add(new LogEntry(2, new Add(2).serialize()), 2);


        RequestVote req = new RequestVote(3, 3, 3, peers.getFirst().id);
        RequestVoteEvent event = new RequestVoteEvent(req, peerChannel);

        follower.handle(event);

        RequestVoteReply reply = peerChannel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, true), reply);
        assertEquals(node.state, NodeState.FOLLOWER());
    }

    @Test
    public void only_grant_one_vote(){
        node.currentTerm = 0;
        follower.handle(new PeerConnectedEvent(peers.getFirst()));
        follower.handle(new PeerConnectedEvent(peers.getLast()));

        RequestVote req = new RequestVote(1, 0, 0, peers.getFirst().id);
        RequestVoteEvent event = new RequestVoteEvent(req, peerChannel);

        follower.handle(event);

        RequestVoteReply reply = peerChannel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, true), reply);


        req = new RequestVote(1, 0, 0, peers.getLast().id);
        event = new RequestVoteEvent(req, peerChannel);

        follower.handle(event);

        reply = peerChannel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);


        assertEquals(node.state, NodeState.FOLLOWER());

    }

    @Test
    @SneakyThrows
    public void redirect_client_command_to_leader(){
        node.currentTerm = 1;
        node.leaderId = leaderId;

        byte[] command = new Set(5).serialize();
        Request request = new Request(command);
        ReceivedRequestEvent event = new ReceivedRequestEvent(request, nodeChannel);

        follower.handle(event);

        Redirect reply = nodeChannel.readOutbound();

        assertEquals(new Redirect(leaderId, request), reply);
    }
}
