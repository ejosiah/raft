package com.josiahebhomenye.raft.log;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.test.support.LogDomainSupport;
import com.josiahebhomenye.test.support.StateDataSupport;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class LogTest implements StateDataSupport, LogDomainSupport {

    Log log;

    @Before
    public void setup(){
        log = new Log("log.dat", Command.SIZE);
    }

    @After
    @SneakyThrows
    public void tearDown(){
        log.close();
        delete("log.dat");
    }

    @Test
    public void check_that_we_can_read_and_write_to_log(){

        IntStream.range(0, logEntries().size()).forEach(i -> log.add(logEntries().get(i), i+1));
        IntStream.range(0, logEntries().size()).forEach(i -> assertEquals(log.get(i+1), logEntries().get(i)));
    }

    @Test
    public void check_that_can_read_and_write_to_random_points_in_the_log(){
        IntStream.range(1, logEntries().size()).forEach(i -> {
            int id = new Random().nextInt(100);
            log.add(logEntries().get(i), id+1);
            assertEquals(log.get(id+1), logEntries().get(i));
        });
    }

    @Test
    public void check_that_we_can_retrieve_last_entry(){
        IntStream.range(0, logEntries().size()).forEach(i -> log.add(logEntries().get(i), i+1));

        assertEquals(logEntries().getLast(), log.lastEntry());
    }

    @Test
    public void check_that_we_can_retrieve_index_of_last_entry(){
        IntStream.range(0, logEntries().size()).forEach(i -> log.add(logEntries().get(i), i+1));

        assertEquals(logEntries().size(), log.getLastIndex());
    }

    @Test(expected = IllegalArgumentException.class)
    public void null_command_should_throw_fail(){
        log.add(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negative_indexes_should_fail(){
        log.add(logEntries().get(0), -1);
    }

    @Test
    public void delete_entries_from_giving_index(){
        IntStream.range(0, logEntries().size()).forEach(i -> log.add(logEntries().get(i), i+1));

        log.deleteFromIndex(4);
        assertEquals(3, log.size());
        IntStream.range(0, 3).forEach(i -> assertEquals( logEntries().get(i), log.get(i+1)) );
    }

    @Test
    public void delete_of_index_above_size_of_log_does_nothing(){
        IntStream.range(0, logEntries().size()).forEach(i -> log.add(logEntries().get(i), i+1));

        log.deleteFromIndex(8);
        assertEquals(logEntries().size(), log.size());
        IntStream.range(0, logEntries().size()).forEach(i -> assertEquals( logEntries().get(i), log.get(i+1)) );
    }

    @Test
    public void retrieve_entries_from_a_giving_index(){
        IntStream.range(0, logEntries().size()).forEach(i -> log.add(logEntries().get(i), i+1));

        List<LogEntry> expected = logEntries().stream().skip(2).collect(Collectors.toList());
        List<LogEntry> actual = log.entriesFrom(3);

        assertEquals(expected, actual);
    }
}