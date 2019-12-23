package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.client.Request;
import com.josiahebhomenye.raft.event.Event;
import io.netty.channel.Channel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(fluent = true)
@EqualsAndHashCode(callSuper=false, exclude = {"peer"})
public class ReceivedRequestEvent extends Event {
    private Channel sender;
    private Request request;

    public ReceivedRequestEvent(Request request, Channel sender){
        super(sender);
        this.request = request;
        this.sender = sender;
    }
}
