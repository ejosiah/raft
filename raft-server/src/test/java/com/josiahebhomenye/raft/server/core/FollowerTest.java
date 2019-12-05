package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.RequestVoteReply;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Multiply;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.comand.Subtract;
import com.josiahebhomenye.raft.server.event.ElectionTimeoutEvent;
import com.josiahebhomenye.raft.server.event.RequestVoteEvent;
import lombok.SneakyThrows;
import org.junit.Test;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class FollowerTest extends NodeStateTest{

    Follower follower;

    @Override
    public NodeState initializeState() {
        follower = new Follower();
        follower.set(node);
        node.state = follower;

        return follower;
    }

    @Test
    public void initialization_should_schedule_election_timeout() throws Exception{
        follower.init();

        Thread.sleep(400L);

        ElectionTimeoutEvent expected = new ElectionTimeoutEvent(null, node.id);
        ElectionTimeoutEvent actual = userEventCapture.get();

        assertEquals(expected, actual);
    }

    @Test
    public void follower_should_transition_to_candidate_when_no_heartbeat_received_before_election_timeout(){
        Instant lastHeartbeat = Instant.now().minus(Duration.ofMinutes(5));
        node.lastheartbeat = lastHeartbeat;
        follower.handle(new ElectionTimeoutEvent(lastHeartbeat, node.id));

        assertEquals(node.state, NodeState.CANDIDATE);
    }

    @Test
    public void follower_should_transition_to_candidate_on_last_heartbeat_is_not_set(){
        node.lastheartbeat = null;
        follower.handle(new ElectionTimeoutEvent(null, node.id));

        assertEquals(node.state, NodeState.CANDIDATE);
    }

    @Test
    @SneakyThrows
    public void follower_should_schedule_a_new_election_time_when_heartbeat_received_after_election_timeout(){
        node.lastheartbeat = Instant.now();
        Instant lastHeartbeat = node.lastheartbeat.minusMillis(1000);

        follower.handle(new ElectionTimeoutEvent(lastHeartbeat, node.id));

        Thread.sleep(400L);

        ElectionTimeoutEvent expected = new ElectionTimeoutEvent(node.lastheartbeat, node.id);
        ElectionTimeoutEvent actual = userEventCapture.get();

        assertEquals(node.state, follower);
        assertEquals(expected, actual);
    }

    @Test
    @SneakyThrows
    public void Follower_should_not_grant_vote_when_requestors_term_is_less_than_current_term(){
        node.currentTerm = 2;
        InetSocketAddress rquestorId = new InetSocketAddress("localhost", 9001);
        Peer peer = new Peer(rquestorId, channel, group);
        peer.channel = channel;
        node.activePeers.put(rquestorId, peer);

        RequestVote req = new RequestVote(1, 3, 1, rquestorId);
        RequestVoteEvent event = new RequestVoteEvent(req, channel);

        follower.handle(event);

        RequestVoteReply reply = channel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
    }

    @Test
    @SneakyThrows
    public void follower_should_not_grant_vote_when_already_voted_for_another_candidate(){
        node.currentTerm = 2;
        node.votedFor = new InetSocketAddress("localhost", 9001);
        InetSocketAddress rquestorId = new InetSocketAddress("localhost", 9001);
        Peer peer = new Peer(rquestorId, channel, group);
        peer.channel = channel;
        node.activePeers.put(rquestorId, peer);

        RequestVote req = new RequestVote(3, 3, 1, rquestorId);
        RequestVoteEvent event = new RequestVoteEvent(req, channel);

        follower.handle(event);

        RequestVoteReply reply = channel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
    }

    @Test
    @SneakyThrows
    public void follower_should_not_grant_vote_when_candidates_last_log_term_entry_is_not_up_to_date(){
        node.currentTerm = 3;
        InetSocketAddress rquestorId = new InetSocketAddress("localhost", 9001);
        Peer peer = new Peer(rquestorId, channel, group);
        peer.channel = channel;
        node.activePeers.put(rquestorId, peer);
        node.log.add(new LogEntry(1, new Set(5)), 1);
        node.log.add(new LogEntry(2, new Add(2)), 2);
        node.log.add(new LogEntry(3, new Multiply(3)), 3);
        node.log.add(new LogEntry(3, new Subtract(1)), 4);

        RequestVote req = new RequestVote(3, 3, 1, rquestorId);
        RequestVoteEvent event = new RequestVoteEvent(req, channel);

        follower.handle(event);

        RequestVoteReply reply = channel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
    }

    @Test
    @SneakyThrows
    public void follower_should_not_grant_vote_when_candidates_last_log_index_is_not_up_to_date(){
        node.currentTerm = 3;
        InetSocketAddress rquestorId = new InetSocketAddress("localhost", 9001);
        Peer peer = new Peer(rquestorId, channel, group);
        peer.channel = channel;
        node.activePeers.put(rquestorId, peer);
        node.log.add(new LogEntry(1, new Set(5)), 1);
        node.log.add(new LogEntry(2, new Add(2)), 2);
        node.log.add(new LogEntry(3, new Multiply(3)), 3);
        node.log.add(new LogEntry(3, new Subtract(1)), 4);

        RequestVote req = new RequestVote(3, 3, 3, rquestorId);
        RequestVoteEvent event = new RequestVoteEvent(req, channel);

        follower.handle(event);

        RequestVoteReply reply = channel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, false), reply);
    }

    @Test
    @SneakyThrows
    public void grant_vote_when_all_conditions_are_met(){
        node.currentTerm = 2;
        InetSocketAddress rquestorId = new InetSocketAddress("localhost", 9001);
        Peer peer = new Peer(rquestorId, channel, group);
        peer.channel = channel;
        node.activePeers.put(rquestorId, peer);
        node.log.add(new LogEntry(1, new Set(5)), 1);
        node.log.add(new LogEntry(2, new Add(2)), 2);


        RequestVote req = new RequestVote(3, 3, 3, rquestorId);
        RequestVoteEvent event = new RequestVoteEvent(req, channel);

        follower.handle(event);

        RequestVoteReply reply = channel.readOutbound();
        assertEquals(new RequestVoteReply(node.currentTerm, true), reply);
    }
}
