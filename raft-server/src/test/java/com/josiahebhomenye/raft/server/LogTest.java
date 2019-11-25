package com.josiahebhomenye.raft.server;

import com.josiahebhomenye.raft.Divide;
import com.josiahebhomenye.raft.comand.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class LogTest {

    List<Command> commands = new ArrayList<Command>(){
        {
            add(new Set(0));
            add(new Add(5));
            add(new Add(3));
            add(new Subtract(1));
            add(new Multiply(10));
            add(new Divide(2));
        }
    };

    @Test
    public void check_that_we_can_read_and_write_to_log(){
        Log log = new Log("log.dat");


        IntStream.range(0, commands.size()).forEach(i -> log.add(commands.get(i), i));
        IntStream.range(0, commands.size()).forEach(i -> assertEquals(log.get(i), commands.get(i)));
    }

    @Test
    public void check_that_can_read_and_write_to_random_points_in_the_log(){
        Log log = new Log("log.dat");
        IntStream.range(0, commands.size()).forEach( i -> {
            int id = new Random().nextInt(100);
            log.add(commands.get(i), id);
            assertEquals(log.get(id), commands.get(i));
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void null_command_should_throw_fail(){
        Log log = new Log("log.dat");
        log.add(null, 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negative_indexs_should_fail(){
        Log log = new Log("log.dat");
        log.add(commands.get(0), -1);
    }

}