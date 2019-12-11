package com.josiahebhomenye.raft.server.event;

import com.josiahebhomenye.raft.RequestVote;
import com.josiahebhomenye.raft.RequestVoteReply;
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
public class RequestVoteReplyEvent extends Event {
    private RequestVoteReply reply;
    private Channel sender;

    public RequestVoteReplyEvent(RequestVoteReply reply, Channel sender){
        super(sender.remoteAddress());
        this.reply = reply;
    }
}
