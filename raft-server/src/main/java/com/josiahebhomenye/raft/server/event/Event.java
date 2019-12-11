package com.josiahebhomenye.raft.server.event;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


@NoArgsConstructor
@Getter
public abstract class Event {
     public SocketAddress source;

    public Event(SocketAddress source){
        this.source = source;
    }
}
