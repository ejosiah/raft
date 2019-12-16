package com.josiahebhomenye.raft;

import com.josiahebhomenye.raft.comand.Command;
import com.josiahebhomenye.raft.comand.Data;
import com.josiahebhomenye.raft.event.StateUpdatedEvent;
import com.josiahebhomenye.raft.event.ApplyEntryEvent;
import com.josiahebhomenye.raft.log.Log;
import com.josiahebhomenye.raft.log.LogEntry;
import com.josiahebhomenye.test.support.LogDomainSupport;
import com.josiahebhomenye.test.support.StateDataSupport;
import com.josiahebhomenye.test.support.UserEventCapture;
import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class DefaultStateManagerTest implements LogDomainSupport, StateDataSupport {

    protected EmbeddedChannel channel;
    protected DefaultStateManager stateManager;
    protected UserEventCapture userEventCapture;

    Log log;

    @Before
    public void setup(){
        log = new Log("log.dat", Command.SIZE);
        stateManager = new DefaultStateManager();
        userEventCapture = new UserEventCapture();
        userEventCapture.ignore(ApplyEntryEvent.class);
        channel = new EmbeddedChannel(userEventCapture, stateManager);
    }

    @After
    public void tearDown(){
        log.close();
        delete("log.path");
    }

    @Test
    public void update_state_when_update_state_event_received(){
        List<LogEntry> entries = logEntries();

        IntStream.rangeClosed(1, entries.size()).forEach(i -> log.add(entries.get(i-1), i));


        for(LogEntry entry : log) {
            channel.pipeline().fireUserEventTriggered(new ApplyEntryEvent(entry, null));

        }

        Data expected = new Data(0);
        for(int i = 0; i < entries.size(); i++){
            StateUpdatedEvent event = userEventCapture.get(i);

            Command.restore(entries.get(i).getCommand()).apply(expected);
            Data actual = event.state();

            assertEquals(String.format("updated state(%s) does not match expected", i), expected, actual);
        }
    }
}