package com.josiahebhomenye.raft.server.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;


@NoArgsConstructor
@Getter
public abstract class Event {
     public InetSocketAddress source;

    public Event(InetSocketAddress source){
        this.source = source;
    }
}
