package com.josiahebhomenye.raft.event;

import com.josiahebhomenye.raft.log.Log;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.SocketAddress;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class UpdateStateEvent extends Event {
    private Log log;

    UpdateStateEvent(Log log, SocketAddress source){
        super(source);
        this.log = log;
    }
}
