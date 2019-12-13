package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.comand.Command;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.SocketAddress;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class ReceivedCommandEvent extends Event {
    private Channel sender;
    private Command command;

    public ReceivedCommandEvent(Command command, Channel sender){
        super(sender.remoteAddress());
        this.command = command;
        this.sender = sender;
    }
}
