package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Set;
import com.josiahebhomenye.raft.server.config.ServerConfig;
import com.typesafe.config.ConfigFactory;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class NodeTest {


    @Before
    @SneakyThrows
    public void setup(){
        try {
            Files.delete(Paths.get("log.dat"));
            Files.delete(Paths.get("state.dat"));
        } catch (NoSuchFileException e) {
            // ignore
        }
    }

    @Test
    public void ensureInitialStateOnFirstBoot(){
        ServerConfig config = new ServerConfig(ConfigFactory.load());
        Node node = new Node(config);
        assertEquals(1, node.getCurrentTerm());
        assertNull(node.getVotedFor());
        assertEquals(NodeState.NULL_STATE, node.getState());
        assertTrue(node.getLog().isEmpty());
    }

    @Test
    @SneakyThrows
    public void ensuredStoredStateIsLoadedOnBoot(){
        Supplier<Void> createStateFile = () -> {
            try(DataOutputStream out = new DataOutputStream(new FileOutputStream("state.dat"))){
                out.writeInt(1);
                out.writeUTF("localhost");
                out.writeInt(8080);
            }catch (Exception e){

            }
            return null;
        };

        Supplier<Void> createLogFile = () -> {
            try(DataOutputStream out = new DataOutputStream(new FileOutputStream("log.dat"))){
                Command command = new Set(5);
                out.writeInt(1);
                out.write(command.serialize());
            }catch (Exception e){

            }

            return null;
        };

        createStateFile.get();
        createLogFile.get();

        ServerConfig config = new ServerConfig(ConfigFactory.load());
        Node node = new Node(config);
        assertEquals(1, node.getCurrentTerm());
        assertEquals(NodeState.NULL_STATE, node.getState());
        assertEquals(new InetSocketAddress("localhost", 8080), node.getVotedFor());
        assertFalse(node.getLog().isEmpty());
        assertEquals(new LogEntry(1, new Set(5)), node.getLog().get(0));

    }

    @Test
    public void should_reply_false_to_AppendEntries_when_term_is_less_than_currentTerm(){
        fail("Not yet implemented!");
    }
}