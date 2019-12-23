package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.Channel;
import lombok.ToString;

import java.net.InetSocketAddress;

@ToString
public class StopEvent extends Event {

    public StopEvent(Channel source){
        super(source);
    }
}
