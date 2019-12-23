package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.client.Response;
import com.josiahebhomenye.raft.rpc.*;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.raft.server.event.*;
import com.josiahebhomenye.test.support.LogDomainSupport;
import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;
import static com.josiahebhomenye.raft.server.core.NodeState.*;

public class LeaderTest extends NodeStateTest implements LogDomainSupport {

    Leader leader;

    @Before
    public void setup(){
        assertTrue(node.isLeader());
    }

    @Override
    public NodeState initializeState() {
        leader =  LEADER();
        leader.set(node);
        return leader;
    }

    @Test
    public void send_heartbeat_on_initialization(){
        node.currentTerm = 1;

        peers.forEach(peer -> node.handle(new PeerConnectedEvent(peer)));

        leader.init();

        ScheduleHeartbeatTimeoutEvent event = userEventCapture.get(0);

        assertEquals(config.heartbeatTimeout.get(), event.timeout());
    }

    @Test
    public void increment_next_index_for_all_available_peers_on_initialization(){
        LinkedList<LogEntry> leaderEntries = leaderEntries();
        node.currentTerm = leaderEntries.getLast().getTerm();

        peers.forEach(peer -> node.handle(new PeerConnectedEvent(peer)));

        peers.forEach(peer -> assertEquals(1, peer.nextIndex));

        leaderEntries.forEach(node.log::add);

        leader.init();

        peers.forEach(peer -> assertEquals(leaderEntries.size() + 1, peer.nextIndex));
    }


    @Test
    public void acknowledge_client_command_request_and_replicate_to_followers() {
        node.currentTerm = 1;
        node.id = leaderId;
        byte[] command = new Set(5).serialize();
        Request request = new Request(command);
        ReceivedRequestEvent event = new ReceivedRequestEvent(request, clientChannel);

        peers.forEach(peer -> node.handle(new PeerConnectedEvent(peer)));

        leader.handle(event);

        Response response = clientChannel.readOutbound();
        assertEquals(request.getId(), response.getCorrelationId());


        byte[] entry = new LogEntry(1, command).serialize();

        AppendEntries expected = new AppendEntries(1, 0, 0, 0, leaderId, Arrays.asList(entry));

        assertEquals(expected, peerChannel.readOutbound());
        assertEquals(expected, peerChannel.readOutbound());
        assertEquals(expected, peerChannel.readOutbound());
        assertEquals(expected, peerChannel.readOutbound());
    }

    @Test
    public void do_not_commit_log_if_unable_to_replicate_to_majority(){
        node.currentTerm = 6;
        node.id = leaderId;
        leaderEntries().forEach(node.log::add);

        peers.stream().limit(2).forEach(peer -> node.trigger(new PeerConnectedEvent(peer)));

        AppendEntriesReplyEvent event = new AppendEntriesReplyEvent(new AppendEntriesReply(1, 0,true), peers.get(0));

        leader.handle(event);
        leader.handle(event.withSender(peers.get(1)).withMsg(event.msg().withSuccess(false)));

        assertFalse(userEventCapture.get(CommitEvent.class).isPresent());
    }

    @Test
    public void commit_log_on_majority_replication(){
        node.currentTerm = 3;
        node.log.add(new LogEntry(1, new Set(3).serialize()), 1);
        node.log.add(new LogEntry(1, new Set(1).serialize()), 2);
        node.log.add(new LogEntry(1, new Set(9).serialize()), 3);

        node.log.add(new LogEntry(2, new Set(2).serialize()), 4);

        node.log.add(new LogEntry(3, new Set(0).serialize()), 5);
        node.log.add(new LogEntry(3, new Set(7).serialize()), 6);
        node.log.add(new LogEntry(3, new Set(5).serialize()), 7);
        node.log.add(new LogEntry(3, new Set(4).serialize()), 8);

        peers.forEach(peer -> node.handle(new PeerConnectedEvent(peer)));

        peers.get(0).nextIndex = 6;
        peers.get(0).matchIndex = 5;

        peers.get(1).nextIndex = 9;
        peers.get(1).matchIndex = 8;

        peers.get(2).nextIndex = 3;
        peers.get(2).matchIndex = 2;

        peers.get(3).nextIndex = 8;
        peers.get(3).matchIndex = 7;

        List<Peer> peerClones = peers.stream().map(Peer::clone).collect(Collectors.toList());

        AppendEntriesReplyEvent event = new AppendEntriesReplyEvent(new AppendEntriesReply(3, 0, true), peers.get(0));
        peers.forEach( peer -> {
            if(peer.matchIndex >= 8){
                leader.handle(event.withSender(peer).withMsg(event.msg().withLastApplied(peer.matchIndex)));
            }else {
                leader.handle(event.withMsg(event.msg().withSuccess(false)));
            }
        });


        CommitEvent expected = new CommitEvent(7L, node.channel);
        CommitEvent actual = userEventCapture.get(CommitEvent.class).get();

        assertEquals(expected, actual);

        IntStream.range(0, peers.size()).forEach(i -> {
            if(peerClones.get(i).matchIndex > 8) {
                assertEquals(peerClones.get(i).matchIndex + 1, peers.get(i).matchIndex);
                assertEquals(peerClones.get(i).nextIndex + 1, peers.get(i).nextIndex);
            }
        });
    }

    @Test
    public void heartbeat_reply_with_no_updates_should_not_change_peer_indexes(){
        LinkedList<LogEntry> leaderEntries = leaderEntries();
        node.commitIndex = leaderEntries.size();
        node.currentTerm = leaderEntries.getLast().getTerm();
        node.lastApplied = leaderEntries.size()/2;
        node.commitIndex = node.lastApplied;

        Peer peer = peers.getFirst();
        peer.matchIndex = 3;
        peer.nextIndex = 4;

        AppendEntriesReplyEvent event = new AppendEntriesReplyEvent(new AppendEntriesReply(node.currentTerm, 0, true), peer);

        leader.handle(event);

        assertFalse(userEventCapture.get(CommitEvent.class).isPresent());

        assertEquals(3, peer.matchIndex);
        assertEquals(4, peer.nextIndex);
    }

    @Test
    public void retry_append_entries_with_decremented_nextIndex_on_rejection(){
        node.currentTerm = 3;
        node.id = leaderId;


        node.log.add(new LogEntry(1, new Set(3).serialize()), 1);
        node.log.add(new LogEntry(1, new Set(1).serialize()), 2);
        node.log.add(new LogEntry(1, new Set(9).serialize()), 3);

        node.log.add(new LogEntry(2, new Set(2).serialize()), 4);

        node.log.add(new LogEntry(3, new Set(0).serialize()), 5);
        node.log.add(new LogEntry(3, new Set(7).serialize()), 6);
        node.log.add(new LogEntry(3, new Set(5).serialize()), 7);
        node.log.add(new LogEntry(3, new Set(4).serialize()), 8);

        peers.forEach(peer -> node.handle(new PeerConnectedEvent(peer)));


        AppendEntriesReplyEvent event = new AppendEntriesReplyEvent(new AppendEntriesReply(3, 0, false), peers.get(0));


        leader.handle(event);

        List<byte[]> missingEntries = new ArrayList<>();
        missingEntries.add(new LogEntry(3, new Set(4).serialize()).serialize());

        AppendEntries expected = new AppendEntries(3, 7, 3, 0, leaderId, missingEntries);
        AppendEntries actual = peerChannel.readOutbound();

        assertEquals(expected, actual);

    }

    @Test
    public void heartbeat_schedule_is_canceled_when_transition_away_from_leader(){
        leader.init();

        leader.transitionTo(FOLLOWER());

        Optional<CancelHeartbeatTimeoutEvent> event = userEventCapture.get(CancelHeartbeatTimeoutEvent.class);

        assertTrue("CancelHeartbeatTimeoutEvent was not triggered", event.isPresent());
    }

    @Test
    public void grant_vote_for_candidate_of_higher_terms_and_step_down(){
        node.currentTerm = 1;
        node.votedFor = node.id;
        leader.init();

        RequestVote request = new RequestVote(2, 0, 0, leaderId);
        leader.handle(new RequestVoteEvent(request, peerChannel));

        RequestVoteReply actual = peerChannel.readOutbound();
        RequestVoteReply expected = new RequestVoteReply(1, true);

        assertEquals("vote not granted", expected, actual);
        assertTrue("did not step down for next term", node.isFollower());

    }

    @Test
    public void dont_grant_vote_to_candidate_for_current_term(){
        node.currentTerm = 1;
        node.votedFor = node.id;

        RequestVote request = new RequestVote(1, 0, 0, leaderId);
        leader.handle(new RequestVoteEvent(request, peerChannel));

        RequestVoteReply actual = peerChannel.readOutbound();
        RequestVoteReply expected = new RequestVoteReply(1, false);

        assertEquals("vote granted", expected, actual);
        assertTrue("stepped down as leader", node.isLeader());

    }
}
