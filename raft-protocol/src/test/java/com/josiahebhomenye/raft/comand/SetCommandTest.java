package com.josiahebhomenye.raft.comand;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SetCommandTest extends CommandSerializationTest {

    @Test
    public void executingSetShouldBeSuccessful(){
        val data = new Data(5);
        val command = new Set(25);
        command.apply(data);
        assertEquals(data.value(), 25);
    }

    @Test
    public void addCommandShouldGiveTheCorrectId(){
        assertEquals(new Set(0).id(), Command.SET);
    }

    @Override
    Command get() {
        return new Set(15);
    }
}