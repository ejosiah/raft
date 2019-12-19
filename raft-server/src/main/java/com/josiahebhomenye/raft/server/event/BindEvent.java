package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class BindEvent extends Event {
    private Channel channel;

    public BindEvent(Channel channel){
        super(channel.localAddress());
        this.channel = channel;
    }
}
