package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.comand.Command;
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
public class ReceivedCommandEvent extends Event {
    private Channel sender;
    private byte[] command;

    public ReceivedCommandEvent(byte[] command, Channel sender){
        super(sender.remoteAddress());
        this.command = command;
        this.sender = sender;
    }
}
