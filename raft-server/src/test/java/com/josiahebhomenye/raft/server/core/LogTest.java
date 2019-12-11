package com.josiahebhomenye.raft.server.core;

import com.josiahebhomenye.raft.Divide;
import com.josiahebhomenye.raft.comand.*;
import com.josiahebhomenye.raft.server.core.Log;
import com.josiahebhomenye.raft.server.core.LogEntry;
import lombok.SneakyThrows;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class LogTest {

    LinkedList<LogEntry> logEntries = new LinkedList<LogEntry>(){
        {
            add(new LogEntry(1, new Set(0)));
            add(new LogEntry(1, new Add(5)));
            add(new LogEntry(1, new Add(3)));
            add(new LogEntry(1, new Subtract(1)));
            add(new LogEntry(1, new Multiply(10)));
            add(new LogEntry(1, new Divide(2)));
        }
    };

    @Before
    @SneakyThrows
    public void setup(){
        new Log("log.dat").clear();
    }

    @Test
    public void check_that_we_can_read_and_write_to_log(){
        Log log = new Log("log.dat");

        IntStream.range(0, logEntries.size()).forEach(i -> log.add(logEntries.get(i), i+1));
        IntStream.range(0, logEntries.size()).forEach(i -> assertEquals(log.get(i+1), logEntries.get(i)));
    }

    @Test
    public void check_that_can_read_and_write_to_random_points_in_the_log(){
        Log log = new Log("log.dat");
        IntStream.range(1, logEntries.size()).forEach(i -> {
            int id = new Random().nextInt(100);
            log.add(logEntries.get(i), id+1);
            assertEquals(log.get(id+1), logEntries.get(i));
        });
    }

    @Test
    public void check_that_we_can_retrieve_last_entry(){
        Log log = new Log("log.dat");
        IntStream.range(0, logEntries.size()).forEach(i -> log.add(logEntries.get(i), i+1));

        assertEquals(logEntries.getLast(), log.lastEntry());
    }

    @Test
    public void check_that_we_can_retrieve_index_of_last_entry(){
        Log log = new Log("log.dat");
        IntStream.range(0, logEntries.size()).forEach(i -> log.add(logEntries.get(i), i+1));

        assertEquals(logEntries.size(), log.getLastIndex());
    }

    @Test(expected = IllegalArgumentException.class)
    public void null_command_should_throw_fail(){
        Log log = new Log("log.dat");
        log.add(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negative_indexes_should_fail(){
        Log log = new Log("log.dat");
        log.add(logEntries.get(0), -1);
    }

    @Test
    public void delete_entries_from_giving_index(){
        Log log = new Log("log.dat");
        IntStream.range(0, logEntries.size()).forEach(i -> log.add(logEntries.get(i), i+1));

        log.deleteFromIndex(4);
        assertEquals(3, log.size());
        IntStream.range(0, 3).forEach(i -> assertEquals( logEntries.get(i), log.get(i+1)) );
    }

    @Test
    public void delete_of_index_above_size_of_log_does_nothing(){
        Log log = new Log("log.dat");
        IntStream.range(0, logEntries.size()).forEach(i -> log.add(logEntries.get(i), i+1));

        log.deleteFromIndex(8);
        assertEquals(logEntries.size(), log.size());
        IntStream.range(0, logEntries.size()).forEach(i -> assertEquals( logEntries.get(i), log.get(i+1)) );
    }
}