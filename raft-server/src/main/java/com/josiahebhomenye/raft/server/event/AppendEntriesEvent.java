package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.AppendEntries;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.net.InetSocketAddress;


@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false)
public class AppendEntriesEvent extends Event{
    private AppendEntries msg;
    private Channel sender;

    public AppendEntriesEvent(AppendEntries msg, Channel sender){
        super(msg.getLeaderId());
        this.msg = msg;
        this.sender = sender;
    }
}
