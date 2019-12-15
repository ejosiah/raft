package com.josiahebhomenye.raft.comand;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DivideCommandTest extends CommandSerializationTest {

    @Test
    public void executingDivideShouldBeSuccessful(){
        val data = new Data(25);
        val command = new Divide(5);
        command.apply(data);
        assertEquals(data.value(), 5);
    }

    @Test
    public void divideCommandShouldGiveTheCorrectId(){
        assertEquals(new Divide(0).id(), Command.DIVIDE);
    }

    @Override
    Command get() {
        return new Divide(15);
    }
}