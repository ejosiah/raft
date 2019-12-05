package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.AppendEntries;
import com.josiahebhomenye.raft.AppendEntriesReply;
import com.josiahebhomenye.raft.comand.Add;
import com.josiahebhomenye.raft.comand.Set;
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
    public void setup(){
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
        channel = new EmbeddedChannel(userEventCapture, node);
        node.channel = channel;
        state = initializeState();
        leaderId = new InetSocketAddress("localhost", 9000);
    }

    @Test
    public void reply_false_to_append_entry_message_if_leaders_term_is_behind(){
        if(state == NodeState.LEADER) return;
        node.currentTerm = 2;
        AppendEntries entries = AppendEntries.heartbeat(node.currentTerm - 1, 0, 0, 0, leaderId);

        state.handle(new AppendEntriesEvent(entries, channel));

        AppendEntriesReply expected = new AppendEntriesReply(2, false);
        AppendEntriesReply actual = channel.readOutbound();

        assertEquals(expected, actual);

    }

    @Test
    public void replay_false_to_append_entry_message_if_log_does_not_contain_an_entry_with_previous_log_term(){
        if(state == NodeState.LEADER) return;

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

    public abstract NodeState initializeState();
}
