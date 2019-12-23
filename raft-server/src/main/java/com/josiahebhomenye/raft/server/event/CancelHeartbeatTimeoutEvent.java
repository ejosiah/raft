package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.With;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;

@Data
@With
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class CancelHeartbeatTimeoutEvent extends Event {

    public CancelHeartbeatTimeoutEvent(Channel source){
        super(source);
    }
}
