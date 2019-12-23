package com.josiahebhomenye.raft.event;

import io.netty.channel.Channel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.net.SocketAddress;


@NoArgsConstructor
@Getter
public abstract class Event {
    public Channel source;

    public Event(Channel source){
        this.source = source;
    }

    public <T extends Event> T as(Class<T> clazz){
        return clazz.cast(this);
    }
}
