package com.josiahebhomenye.raft.comand;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubstractCommandTest extends CommandSerializationTest {

    @Test
    public void executingSubtractShouldBeSuccessful(){
        val data = new Data(5);
        val command = new Subtract(25);
        command.apply(data);
        assertEquals(data.value(), -20);
    }

    @Test
    public void subtractCommandShouldGiveTheCorrectId(){
        assertEquals(new Subtract(0).id(), Command.SUBTRACT);
    }

    @Override
    Command get() {
        return new Subtract(15);
    }
}