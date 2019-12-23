package com.josiahebhomenye.raft.event;

import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.SocketAddress;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper=false)
public class StateUpdatedEvent extends Event {
    private Object state;

    public StateUpdatedEvent(Object state, Channel source){
        super(source);
        this.state = state;
    }

    @SuppressWarnings("unchecked")
    public <T> T state(){
        return (T)state;
    }
}
