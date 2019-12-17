package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import lombok.ToString;

import java.net.InetSocketAddress;

@ToString
public class StopEvent extends Event {

    public StopEvent(InetSocketAddress source){
        super(source);
    }
}
