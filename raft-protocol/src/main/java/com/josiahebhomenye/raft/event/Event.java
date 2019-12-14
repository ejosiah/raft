package com.josiahebhomenye.raft.event;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.InetSocketAddress;
import java.net.SocketAddress;


@NoArgsConstructor
@Getter
@EqualsAndHashCode
public abstract class Event {
     public SocketAddress source;

    public Event(SocketAddress source){
        this.source = source;
    }
}
