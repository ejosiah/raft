package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.rpc.RequestVoteReply;
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
public class RequestVoteReplyEvent extends Event {
    private RequestVoteReply reply;
    private Channel sender;

    public RequestVoteReplyEvent(RequestVoteReply reply, Channel sender){
        super(sender);
        this.reply = reply;
        this.sender = sender;
    }

    public boolean voteGranted(){
        return reply.isVoteGranted();
    }
}
