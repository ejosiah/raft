package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.comand.Subtract;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.josiahebhomenye.raft.server.event.AppendEntriesEvent;
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

import static org.junit.Assert.*;

public abstract class NodeStateTest {

    EventLoopGroup group;
    UserEventCapture userEventCapture;
    EmbeddedChannel channel;
    NodeState state;
    Node node;
    InetSocketAddress leaderId;

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
    }

    @Test
    public void reply_false_to_append_entry_message_if_leaders_term_is_behind(){
        if(state == NodeState.LEADER || state == NodeState.CANDIDATE) return;
        node.currentTerm = 2;
        AppendEntries entries = AppendEntries.heartbeat(node.currentTerm - 1, 0, 0, 0, leaderId);

        state.handle(new AppendEntriesEvent(entries, channel));

        AppendEntriesReply expected = new AppendEntriesReply(2, false);
        AppendEntriesReply actual = channel.readOutbound();

        assertEquals(expected, actual);

    }

    @Test
    public void reply_false_to_append_entry_message_if_log_does_not_contain_an_entry_with_previous_log_term(){
        if(state == NodeState.LEADER || state == NodeState.CANDIDATE) return;

        node.currentTerm = 4;

        node.log.add(new LogEntry(1, new Set(5)), 1);
        node.log.add(new LogEntry(2, new Add(1)), 2);
        node.log.add(new LogEntry(2, new Set(3)), 3);
        node.log.add(new LogEntry(3, new Set(2)), 4);

        AppendEntries entries = AppendEntries.heartbeat(node.currentTerm, 3, 3, 0, leaderId);

        state.handle(new AppendEntriesEvent(entries, channel));

        AppendEntriesReply expected = new AppendEntriesReply(node.currentTerm, false);
        AppendEntriesReply actual = channel.readOutbound();

        assertEquals(expected, actual);
    }

    @Test
    public void delete_exiting_entries_that_conflict_with_new_entries(){
        if(state == NodeState.LEADER || state == NodeState.CANDIDATE) return;

        node.currentTerm = 2;
        long leaderTerm = 3;
        long leaderCommit = 3;
        long prevLogIndex = 3;
        long prevLogTerm = 2;

        LogEntry conflictingEntry = new LogEntry(2, new Set(2));

        node.log.add(new LogEntry(1, new Set(5)), 1);
        node.log.add(new LogEntry(2, new Add(1)), 2);
        node.log.add(new LogEntry(2, new Set(3)), 3);
        node.log.add(conflictingEntry, 4);
        node.log.add(new LogEntry(2, new Add(1)), 5);

        List<byte[]> entries = new ArrayList<>();
        entries.add(new LogEntry(3, new Add(3)).serialize());

        AppendEntries appendEntries = new AppendEntries(leaderTerm, prevLogIndex, prevLogTerm, leaderCommit, leaderId, entries);

        state.handle(new AppendEntriesEvent(appendEntries, channel));

        assertEquals(4, node.log.size());
        assertNotEquals(conflictingEntry, node.log.get(4)); // conflicting entry removed
        assertEquals(new LogEntry(3, new Add(3)), node.log.lastEntry()); // and replaced with entry from leader
        assertEquals(leaderCommit, node.commitIndex);

        AppendEntriesReply expected = new AppendEntriesReply(leaderTerm, true);
        AppendEntriesReply actual = channel.readOutbound();

        assertEquals(expected, actual);
    }

    public abstract NodeState initializeState();
}
