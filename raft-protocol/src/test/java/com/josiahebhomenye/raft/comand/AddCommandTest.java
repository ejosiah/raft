package com.josiahebhomenye.raft.comand;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.*;

public class AddCommandTest {

    @Test
    public void executingAddShouldBeSuccessful(){
        val data = new Data(5);
        val command = new Add(25);
        command.apply(data);
        assertEquals(data.value(), 30);
    }

    @Test
    public void addCommandShouldGiveTheCorrectId(){
        assertEquals(new Add(0).id(), Command.ADD);
    }
}