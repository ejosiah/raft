package com.josiahebhomenye.raft.comand;

import lombok.val;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiplyCommandTest extends CommandSerializationTest {

    @Test
    public void executingMultiplyShouldBeSuccessful(){
        val data = new Data(4);
        val command = new Multiply(25);
        command.apply(data);
        assertEquals(data.value(), 100);
    }

    @Test
    public void multiplyCommandShouldGiveTheCorrectId(){
        assertEquals(new Multiply(0).id(), Command.MULTIPLY);
    }

    @Override
    Command get() {
        return new Multiply(15);
    }
}