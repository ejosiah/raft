package com.josiahebhomenye.raft.comand;

import org.junit.Test;

import static org.junit.Assert.*;

public abstract class CommandSerializationTest {

    abstract Command get();

    @Test
    public void assertThatCommandCanBeSerialized(){
        Command command = get();
        byte[] data = command.serialize();
        Command restoredCommand = Command.restore(data);
        assertEquals(restoredCommand, command);
    }
}